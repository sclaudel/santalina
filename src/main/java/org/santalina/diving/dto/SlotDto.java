package org.santalina.diving.dto;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.WaitingListEntry;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

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
            @JsonFormat(pattern = "HH:mm") LocalTime endTime
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
            Long createdById,
            String createdByName,
            List<SlotDiverResponse> divers,
            boolean registrationOpen,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime registrationOpensAt,
            long waitingListCount
    ) {
        public static SlotResponse from(DiveSlot slot) {
            List<SlotDiverResponse> divers = SlotDiver.findBySlot(slot.id)
                    .stream().map(SlotDiverResponse::from).toList();
            long waitingListCount = WaitingListEntry.countBySlot(slot.id);
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
                    slot.createdBy != null ? slot.createdBy.id : null,
                    slot.createdBy != null ? slot.createdBy.fullName() : null,
                    divers,
                    slot.registrationOpen,
                    slot.registrationOpensAt,
                    waitingListCount
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
