package org.santalina.diving.dto;

import org.santalina.diving.domain.Palanquee;
import org.santalina.diving.domain.PalanqueeMember;
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

    /** diverId obligatoire ; palanqueeId nullable = désassigner ; fromPalanqueeId = désassigner d'une palanquée spécifique (multi-plongée) */
    public record AssignDiverRequest(
            @NotNull Long diverId,
            Long palanqueeId,
            Long fromPalanqueeId
    ) {}

    /** Liste ordonnée des IDs plongeurs de la palanquée */
    public record ReorderRequest(
            @NotNull List<Long> diverIds
    ) {}

    /** Met à jour les aptitudes d'un membre dans une palanquée (surcharge par plongée). */
    public record UpdateMemberAptitudesRequest(String aptitudes) {}

    public record PalanqueeResponse(
            Long id,
            String name,
            int position,
            String depth,
            String duration,
            Long slotDiveId,
            List<SlotDiverResponse> divers
    ) {
        public static PalanqueeResponse from(Palanquee p) {
            List<SlotDiverResponse> divers = PalanqueeMember.findByPalanquee(p.id)
                    .stream().map(m -> SlotDiverResponse.fromWithAptitudes(m.diver, m.aptitudes)).toList();
            Long slotDiveId = p.slotDive != null ? p.slotDive.id : null;
            return new PalanqueeResponse(p.id, p.name, p.position, p.depth, p.duration, slotDiveId, divers);
        }
    }
}
