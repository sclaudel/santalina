package org.santalina.diving.dto;

import org.santalina.diving.domain.Palanquee;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PalanqueeDto {

    public record CreatePalanqueeRequest(
            @NotBlank String name
    ) {}

    public record RenamePalanqueeRequest(
            @NotBlank String name,
            String depth,
            String duration
    ) {}

    /** diverId obligatoire ; palanqueeId nullable = désassigner */
    public record AssignDiverRequest(
            @NotNull Long diverId,
            Long palanqueeId
    ) {}

    /** Liste ordonnée des IDs plongeurs de la palanquée */
    public record ReorderRequest(
            @NotNull List<Long> diverIds
    ) {}

    public record PalanqueeResponse(
            Long id,
            String name,
            int position,
            String depth,
            String duration,
            List<SlotDiverResponse> divers
    ) {
        public static PalanqueeResponse from(Palanquee p) {
            List<SlotDiverResponse> divers = SlotDiver.findByPalanquee(p.id)
                    .stream().map(SlotDiverResponse::from).toList();
            return new PalanqueeResponse(p.id, p.name, p.position, p.depth, p.duration, divers);
        }
    }
}
