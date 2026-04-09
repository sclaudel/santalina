package org.santalina.diving.dto;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class SlotDto {

    public record SlotRequest(
            @NotNull LocalDate slotDate,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @NotNull @Min(2) Integer diverCount,
            String title,
            String notes,
            String slotType,
            String club,
            Boolean registrationEnabled,
            Instant registrationOpensAt,
            // Champs récurrence
            Boolean recurring,
            List<Integer> recurringDays,   // 1=Lun … 7=Dim (ISO DayOfWeek)
            LocalDate recurringUntil
    ) {}

    public record UpdateDiverCountRequest(
            @NotNull @Min(2) Integer diverCount
    ) {}

    public record UpdateSlotInfoRequest(
            String title,
            String notes,
            String slotType,
            String club,
            LocalDate slotDate,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            Boolean registrationEnabled,
            Instant registrationOpensAt
    ) {}

    public record SlotResponse(
            Long id,
            LocalDate slotDate,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            int diverCount,
            String title,
            String notes,
            String slotType,
            String club,
            boolean registrationEnabled,
            Instant registrationOpensAt,
            Long createdById,
            String createdByName,
            List<SlotDiverResponse> divers
    ) {
        public static SlotResponse from(DiveSlot slot) {
            // N'expose que les plongeurs CONFIRMED dans la réponse publique
            List<SlotDiverResponse> divers = SlotDiver.findConfirmedBySlot(slot.id)
                    .stream().map(SlotDiverResponse::from).toList();
            return new SlotResponse(
                    slot.id,
                    slot.slotDate,
                    slot.startTime,
                    slot.endTime,
                    slot.diverCount,
                    slot.title,
                    slot.notes,
                    slot.slotType,
                    slot.club,
                    slot.registrationEnabled,
                    slot.registrationOpensAt,
                    slot.createdBy != null ? slot.createdBy.id : null,
                    slot.createdBy != null ? slot.createdBy.fullName() : null,
                    divers
            );
        }
    }

    /** Réponse pour une création simple ou en lot (récurrence) */
    public record BatchSlotResponse(
            List<SlotResponse> slots,
            int created,
            int skipped
    ) {}
}
