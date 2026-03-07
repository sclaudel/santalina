package com.example.diving.service;

import com.example.diving.domain.DiveSlot;
import com.example.diving.domain.SlotDiver;
import com.example.diving.domain.User;
import com.example.diving.domain.UserRole;
import com.example.diving.dto.SlotDto.SlotRequest;
import com.example.diving.dto.SlotDto.SlotResponse;
import com.example.diving.dto.SlotDto.UpdateSlotInfoRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
public class SlotService {


    @Inject
    ConfigService configService;

    /**
     * Retourne les créneaux d'une journée
     */
    public List<SlotResponse> getSlotsByDate(LocalDate date) {
        return DiveSlot.findByDate(date).stream()
                .map(SlotResponse::from)
                .toList();
    }

    /**
     * Retourne les créneaux d'une semaine
     */
    public List<SlotResponse> getSlotsByWeek(LocalDate from) {
        LocalDate to = from.plusDays(6);
        return DiveSlot.findByDateRange(from, to).stream()
                .map(SlotResponse::from)
                .toList();
    }

    /**
     * Retourne les créneaux d'un mois (du 1er au dernier jour)
     */
    public List<SlotResponse> getSlotsByMonth(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        return DiveSlot.findByDateRange(from, to).stream()
                .map(SlotResponse::from)
                .toList();
    }

    /**
     * Retourne un créneau par ID
     */
    public SlotResponse getById(Long id) {
        DiveSlot slot = DiveSlot.findById(id);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");
        return SlotResponse.from(slot);
    }

    /**
     * Créer un créneau (ADMIN ou DIVE_DIRECTOR)
     */
    @Transactional
    public SlotResponse createSlot(SlotRequest request, User currentUser) {
        validateSlotTimes(request.startTime(), request.endTime());
        validateDiverCount(request.diverCount());
        checkCapacity(request.slotDate(), request.startTime(), request.endTime(), request.diverCount(), null);

        DiveSlot slot = new DiveSlot();
        slot.slotDate = request.slotDate();
        slot.startTime = request.startTime();
        slot.endTime = request.endTime();
        slot.diverCount = request.diverCount();
        slot.title = request.title();
        slot.notes = request.notes();
        slot.slotType = request.slotType();
        slot.club = request.club();
        slot.createdBy = currentUser;
        slot.persist();

        return SlotResponse.from(slot);
    }

    /**
     * Modifier les infos textuelles d'un créneau (titre, notes, type, club)
     * - ADMIN : peut modifier n'importe lequel
     * - DIVE_DIRECTOR : seulement les siens
     */
    @Transactional
    public SlotResponse updateSlotInfo(Long id, UpdateSlotInfoRequest request, User currentUser) {
        DiveSlot slot = DiveSlot.findById(id);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        if (currentUser.role == UserRole.DIVE_DIRECTOR &&
                !slot.createdBy.id.equals(currentUser.id)) {
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
        }

        slot.title    = request.title();
        slot.notes    = request.notes();
        slot.slotType = request.slotType();
        slot.club     = request.club();
        slot.persist();

        return SlotResponse.from(slot);
    }

    /**
     * Supprimer un créneau
     * - ADMIN : peut supprimer n'importe lequel
     * - DIVE_DIRECTOR : seulement les siens
     */
    @Transactional
    public void deleteSlot(Long id, User currentUser) {
        DiveSlot slot = DiveSlot.findById(id);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        if (currentUser.role == UserRole.DIVE_DIRECTOR &&
                !slot.createdBy.id.equals(currentUser.id)) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres créneaux");
        }

        slot.delete();
    }

    /**
     * Modifier le nombre de plongeurs d'un créneau
     * - ADMIN : peut modifier n'importe lequel
     * - DIVE_DIRECTOR : seulement les siens
     * - Contrôle que la nouvelle valeur n'est pas inférieure au nombre de plongeurs déjà inscrits
     */
    @Transactional
    public SlotResponse updateDiverCount(Long id, int newDiverCount, User currentUser) {
        DiveSlot slot = DiveSlot.findById(id);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        if (currentUser.role == UserRole.DIVE_DIRECTOR &&
                !slot.createdBy.id.equals(currentUser.id)) {
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
        }

        validateDiverCount(newDiverCount);

        long currentDiverCount = SlotDiver.countBySlot(id);
        if (newDiverCount < currentDiverCount) {
            throw new BadRequestException(
                "Impossible de réduire la capacité à " + newDiverCount +
                " : " + currentDiverCount + " plongeur(s) sont déjà inscrits");
        }

        checkCapacity(slot.slotDate, slot.startTime, slot.endTime, newDiverCount, id);

        slot.diverCount = newDiverCount;
        slot.persist();

        return SlotResponse.from(slot);
    }

    // ---- Validation helpers ----

    private void validateSlotTimes(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("L'heure de début doit être avant l'heure de fin");
        }
        int durationMinutes = (int) java.time.Duration.between(start, end).toMinutes();

        int minHours      = configService.getSlotMinHours();
        int maxHours      = configService.getSlotMaxHours();
        int resolution    = configService.getSlotResolutionMinutes();
        int minMinutes    = minHours * 60;
        int maxMinutes    = maxHours * 60;

        if (durationMinutes < minMinutes) {
            throw new BadRequestException("La durée minimale d'un créneau est de " + minHours + " heure(s)");
        }
        if (durationMinutes > maxMinutes) {
            throw new BadRequestException("La durée maximale d'un créneau est de " + maxHours + " heures");
        }
        if (resolution > 0 && (start.getMinute() % resolution != 0 || end.getMinute() % resolution != 0)) {
            throw new BadRequestException("Les heures doivent être des multiples de " + resolution + " minutes");
        }
    }

    private void validateDiverCount(int count) {
        if (count < 1) {
            throw new BadRequestException("Le nombre de plongeurs doit être d'au moins 1");
        }
        int maxDivers = configService.getMaxDivers();
        if (count > maxDivers) {
            throw new BadRequestException("Le nombre de plongeurs ne peut pas dépasser " + maxDivers);
        }
    }

    /**
     * Vérifie que l'ajout de newDiverCount plongeurs sur la plage [start,end)
     * ne dépasse pas la limite à aucun moment de 15 minutes.
     */
    private void checkCapacity(LocalDate date, LocalTime start, LocalTime end,
                                int newDiverCount, Long excludeSlotId) {
        int maxDivers = configService.getMaxDivers();
        List<DiveSlot> overlapping = DiveSlot.findOverlapping(date, start, end, excludeSlotId);

        int resolution = configService.getSlotResolutionMinutes();
        LocalTime cursor = start;
        while (cursor.isBefore(end)) {
            LocalTime finalCursor = cursor;
            int occupied = overlapping.stream()
                    .filter(s -> !s.startTime.isAfter(finalCursor) && s.endTime.isAfter(finalCursor))
                    .mapToInt(s -> s.diverCount)
                    .sum();
            if (occupied + newDiverCount > maxDivers) {
                throw new BadRequestException(
                        "Capacité maximale de " + maxDivers + " plongeurs dépassée à " + cursor.toString());
            }
            cursor = cursor.plusMinutes(resolution);
        }
    }
}

