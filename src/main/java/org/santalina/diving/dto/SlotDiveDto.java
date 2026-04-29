package org.santalina.diving.dto;

import jakarta.validation.constraints.NotNull;
import org.santalina.diving.domain.SlotDive;

import java.time.LocalTime;

public class SlotDiveDto {

    public record CreateSlotDiveRequest(
            String label,
            LocalTime startTime,
            LocalTime endTime,
            String depth,
            String duration
    ) {}

    public record UpdateSlotDiveRequest(
            String label,
            LocalTime startTime,
            LocalTime endTime,
            String depth,
            String duration
    ) {}

    /** diverId obligatoire ; slotDiveId nullable = désassigner */
    public record AssignPalanqueeToDiveRequest(
            @NotNull Long palanqueeId,
            Long slotDiveId
    ) {}

    public record SlotDiveResponse(
            Long id,
            Long slotId,
            int diveIndex,
            String label,
            LocalTime startTime,
            LocalTime endTime,
            String depth,
            String duration
    ) {
        public static SlotDiveResponse from(SlotDive d) {
            return new SlotDiveResponse(
                    d.id,
                    d.slot.id,
                    d.diveIndex,
                    d.label,
                    d.startTime,
                    d.endTime,
                    d.depth,
                    d.duration
            );
        }
    }
}
