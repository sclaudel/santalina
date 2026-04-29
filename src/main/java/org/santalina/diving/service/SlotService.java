package org.santalina.diving.service;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.SlotDto.BatchSlotResponse;
import org.santalina.diving.dto.SlotDto.SlotRequest;
import org.santalina.diving.dto.SlotDto.SlotResponse;
import org.santalina.diving.dto.SlotDto.UpdateSlotInfoRequest;
import org.santalina.diving.dto.WaitingListDto.UpdateRegistrationRequest;
import org.santalina.diving.mail.BookingNotificationMailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SlotService {

    private static final Logger LOG = Logger.getLogger(SlotService.class);


    @Inject
    ConfigService configService;

    @Inject
    BookingNotificationMailer bookingNotificationMailer;

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
     * Créer un créneau ou plusieurs créneaux (récurrence).
     * Retourne toujours un BatchSlotResponse.
     */
    @Transactional
    public BatchSlotResponse createSlots(SlotRequest request, User currentUser) {
        boolean recurring = Boolean.TRUE.equals(request.recurring())
                && request.recurringDays() != null && !request.recurringDays().isEmpty()
                && request.recurringUntil() != null;

        if (!recurring) {
            // Création simple (comportement original)
            SlotResponse slot = createSlot(request, currentUser);
            return new BatchSlotResponse(List.of(slot), 1, 0);
        }

        // --- Création récurrente (tout ou rien) ---
        LocalDate today     = LocalDate.now();
        LocalDate startDate = request.slotDate();
        LocalDate untilDate = request.recurringUntil();

        // Vérifier la durée max de récurrence
        int maxMonths = configService.getMaxRecurringMonths();
        LocalDate maxUntil = startDate.plusMonths(maxMonths);
        if (untilDate.isAfter(maxUntil)) {
            throw new BadRequestException(
                "La récurrence ne peut pas dépasser " + maxMonths + " mois (jusqu'au " + maxUntil + " maximum)");
        }
        if (untilDate.isBefore(startDate)) {
            throw new BadRequestException("La date de fin de récurrence doit être après la date de début");
        }

        // Validation commune (heures, capacité) — une seule fois
        validateSlotTimes(request.startTime(), request.endTime());
        checkSlotTimeWindow(request.startTime(), request.endTime());
        validateDiverCount(request.diverCount());

        // Jours ISO valides (1=Lun … 7=Dim)
        Set<Integer> days = request.recurringDays().stream()
                .filter(d -> d >= 1 && d <= 7)
                .collect(Collectors.toSet());

        // Calcul des dates candidates (futures uniquement)
        List<LocalDate> candidateDates = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(untilDate)) {
            int dow = cursor.getDayOfWeek().getValue();
            if (days.contains(dow) && !cursor.isBefore(today)) {
                candidateDates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }

        if (candidateDates.isEmpty()) {
            throw new BadRequestException(
                "Aucun créneau récurrent à créer : toutes les dates sélectionnées sont dans le passé ou aucun jour n'est sélectionné");
        }

        // Passe 1 : vérification de TOUS les conflits avant toute création
        List<String> conflictDates = new ArrayList<>();
        for (LocalDate date : candidateDates) {
            try {
                checkExclusiveConflict(date, request.startTime(), request.endTime(), request.slotType(), null);
                checkCapacity(date, request.startTime(), request.endTime(), request.diverCount(), null);
            } catch (BadRequestException ex) {
                conflictDates.add(date.toString());
                LOG.debugf("Conflit détecté le %s : %s", date, ex.getMessage());
            }
        }

        if (!conflictDates.isEmpty()) {
            throw new BadRequestException(
                "Impossible de créer la série récurrente : " + conflictDates.size() +
                " conflit(s) détecté(s) sur : " + String.join(", ", conflictDates));
        }

        // Passe 2 : création de tous les créneaux (sans mail individuel)
        List<DiveSlot> createdSlots = new ArrayList<>();
        for (LocalDate date : candidateDates) {
            DiveSlot slot = persistSlot(request, date, currentUser);
            createdSlots.add(slot);
            LOG.infof("Créneau récurrent créé (id=%d) le %s par %s", slot.id, date, currentUser.email);
        }

        // Un seul mail de récapitulatif pour toute la série
        bookingNotificationMailer.sendRecurringSlotsSummary(createdSlots);

        LOG.infof("Récurrence : %d créneau(x) créé(s) par %s", createdSlots.size(), currentUser.email);
        List<SlotResponse> responses = createdSlots.stream().map(SlotResponse::from).toList();
        return new BatchSlotResponse(responses, createdSlots.size(), 0);
    }

    /**
     * Helper privé : persiste un créneau sans validation ni envoi de mail.
     * Utilisé exclusivement dans le flux récurrent (validation faite en amont).
     */
    private DiveSlot persistSlot(SlotRequest request, LocalDate date, User currentUser) {
        DiveSlot slot = new DiveSlot();
        slot.slotDate   = date;
        slot.startTime  = request.startTime();
        slot.endTime    = request.endTime();
        slot.diverCount = request.diverCount();
        slot.title      = request.title();
        slot.notes      = request.notes();
        slot.slotType   = request.slotType();
        slot.club       = request.club();
        slot.createdBy  = currentUser;
        slot.persist();
        return slot;
    }

    /**
     * Créer un créneau (ADMIN ou DIVE_DIRECTOR) pour une date spécifique.
     */
    @Transactional
    public SlotResponse createSlot(SlotRequest request, LocalDate overrideDate, User currentUser) {
        if (overrideDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Impossible de créer un créneau dans le passé");
        }
        validateSlotTimes(request.startTime(), request.endTime());
        checkSlotTimeWindow(request.startTime(), request.endTime());
        validateDiverCount(request.diverCount());
        checkExclusiveConflict(overrideDate, request.startTime(), request.endTime(), request.slotType(), null);
        checkCapacity(overrideDate, request.startTime(), request.endTime(), request.diverCount(), null);

        DiveSlot slot = persistSlot(request, overrideDate, currentUser);
        LOG.infof("Créneau créé (id=%d) le %s de %s à %s par %s",
                slot.id, slot.slotDate, slot.startTime, slot.endTime, currentUser.email);
        bookingNotificationMailer.sendSlotCreatedNotification(slot);
        return SlotResponse.from(slot);
    }

    /**
     * Créer un créneau (ADMIN ou DIVE_DIRECTOR) — date prise depuis la requête.
     */
    @Transactional
    public SlotResponse createSlot(SlotRequest request, User currentUser) {
        return createSlot(request, request.slotDate(), currentUser);
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

        if (currentUser.role == UserRole.DIVE_DIRECTOR) {
            boolean isCreator = slot.createdBy != null && slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = SlotDiver.isAssignedDirectorByEmail(slot.id, currentUser.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }

        boolean dateOrTimeChanged = request.slotDate() != null
                || request.startTime() != null || request.endTime() != null;
        if (dateOrTimeChanged) {
            LocalDate newDate  = request.slotDate()  != null ? request.slotDate()  : slot.slotDate;
            LocalTime newStart = request.startTime() != null ? request.startTime() : slot.startTime;
            LocalTime newEnd   = request.endTime()   != null ? request.endTime()   : slot.endTime;
            if (newDate.isBefore(LocalDate.now())) {
                throw new BadRequestException("Impossible de déplacer un créneau dans le passé");
            }
            validateSlotTimes(newStart, newEnd);
            checkSlotTimeWindow(newStart, newEnd);
            String newSlotType = request.slotType() != null ? request.slotType() : slot.slotType;
            checkExclusiveConflict(newDate, newStart, newEnd, newSlotType, id);
            checkCapacity(newDate, newStart, newEnd, slot.diverCount, id);
            slot.slotDate  = newDate;
            slot.startTime = newStart;
            slot.endTime   = newEnd;
        }

        slot.title    = request.title();
        slot.notes    = request.notes();
        slot.slotType = request.slotType();
        slot.club     = request.club();
        slot.persist();

        return SlotResponse.from(slot);
    }

    /**
     * Activer / désactiver l'inscription libre sur un créneau.
     * Seul le créateur du créneau (ou un admin) peut modifier ce paramètre.
     */
    @Transactional
    public SlotResponse updateRegistration(Long id, UpdateRegistrationRequest request, User currentUser) {
        DiveSlot slot = DiveSlot.findById(id);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        if (currentUser.role == UserRole.DIVE_DIRECTOR) {
            boolean isCreator = slot.createdBy != null && slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = SlotDiver.isAssignedDirectorByEmail(slot.id, currentUser.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }

        slot.registrationOpen      = request.registrationOpen();
        slot.registrationOpensAt   = request.registrationOpensAt();
        slot.requiresAttachments   = request.requiresAttachments();
        slot.persist();

        LOG.infof("Inscriptions libres %s (id=%d) par %s",
                slot.registrationOpen ? "activées" : "désactivées", id, currentUser.email);
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

        if (currentUser.role == UserRole.DIVE_DIRECTOR) {
            boolean isCreator = slot.createdBy != null && slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = SlotDiver.isAssignedDirectorByEmail(slot.id, currentUser.email);
            if (!isCreator && !isAssignedDP) {
                LOG.warnf("Suppression refusée : %s tente de supprimer le créneau id=%d qui ne lui appartient pas",
                        currentUser.email, id);
                throw new ForbiddenException("Vous ne pouvez supprimer que vos propres créneaux");
            }
        }

        LOG.infof("Créneau supprimé (id=%d) par %s", id, currentUser.email);
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

        if (currentUser.role == UserRole.DIVE_DIRECTOR) {
            boolean isCreator = slot.createdBy != null && slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = SlotDiver.isAssignedDirectorByEmail(slot.id, currentUser.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
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

    /**
     * Vérifie que les heures du créneau (startTime) respectent la fenêtre horaire
     * configurée par l'admin. La restriction porte sur les heures du créneau lui-même,
     * indépendamment de l'heure à laquelle la demande est soumise (les plongeurs
     * peuvent s'inscrire sur un créneau à n'importe quel moment de la journée).
     * booking.open.hour  = -1 → pas de restriction d'ouverture
     * booking.close.hour = -1 → pas de restriction de fermeture
     */
    private void checkSlotTimeWindow(LocalTime startTime, LocalTime endTime) {
        int openHour  = configService.getBookingOpenHour();
        int closeHour = configService.getBookingCloseHour();

        if (openHour == -1 && closeHour == -1) return;

        if (openHour != -1 && startTime.getHour() < openHour) {
            throw new BadRequestException(
                "Le créneau ne peut pas démarrer avant " + String.format("%02d:00", openHour));
        }
        if (closeHour != -1 && startTime.getHour() >= closeHour) {
            throw new BadRequestException(
                "Le créneau ne peut pas démarrer à partir de " + String.format("%02d:00", closeHour));
        }
    }

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
        if (count < 2) {
            throw new BadRequestException("Le nombre de plongeurs doit être d'au moins 2");
        }
        int maxDivers = configService.getMaxDivers();
        if (count > maxDivers) {
            throw new BadRequestException("Le nombre de plongeurs ne peut pas dépasser " + maxDivers);
        }
    }

    /**
     * Vérifie les conflits de types exclusifs :
     * - Si le nouveau créneau est d'un type exclusif, aucun créneau ne peut se chevaucher.
     * - Si un créneau existant chevauchant est d'un type exclusif, la création est bloquée.
     */
    private void checkExclusiveConflict(LocalDate date, LocalTime start, LocalTime end,
                                         String slotType, Long excludeSlotId) {
        List<String> exclusiveTypes = configService.getExclusiveSlotTypes();
        if (exclusiveTypes.isEmpty()) return;

        List<DiveSlot> overlapping = DiveSlot.findOverlapping(date, start, end, excludeSlotId);
        if (overlapping.isEmpty()) return;

        // Le nouveau créneau est d'un type exclusif → aucun chevauchement toléré
        if (slotType != null && exclusiveTypes.contains(slotType)) {
            throw new BadRequestException(
                "Le type \"" + slotType + "\" est exclusif : aucun autre créneau ne peut se chevaucher sur ce créneau");
        }

        // Un créneau existant est d'un type exclusif → le nouveau est bloqué
        overlapping.stream()
            .filter(s -> s.slotType != null && exclusiveTypes.contains(s.slotType))
            .findFirst()
            .ifPresent(s -> { throw new BadRequestException(
                "Un créneau de type exclusif \"" + s.slotType + "\" occupe déjà ce créneau horaire"); });
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
