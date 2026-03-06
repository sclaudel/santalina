package com.example.diving.dto;

import com.example.diving.domain.DiveSlot;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public class SlotDto {

    public record SlotRequest(
            @NotNull LocalDate slotDate,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @NotNull @Min(1) @Max(25) Integer diverCount,
            String title,
            String notes
    ) {}

    public record SlotResponse(
            Long id,
            LocalDate slotDate,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            int diverCount,
            String title,
            String notes,
            Long createdById,
            String createdByName
    ) {
        public static SlotResponse from(DiveSlot slot) {
            return new SlotResponse(
                    slot.id,
                    slot.slotDate,
                    slot.startTime,
                    slot.endTime,
                    slot.diverCount,
                    slot.title,
                    slot.notes,
                    slot.createdBy != null ? slot.createdBy.id : null,
                    slot.createdBy != null ? slot.createdBy.name : null
            );
        }
    }
}

