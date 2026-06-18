package org.santalina.diving.dto;

import org.santalina.diving.domain.Palanquee;
import org.santalina.diving.domain.PalanqueeMember;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
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

    /** Met à jour la fonction d'un membre dans une palanquée. */
    public record UpdateMemberFonctionRequest(String fonction) {}

    /** Réponse pour un membre de palanquée incluant aptitudes et fonction surcharge. */
    public record PalanqueeMemberResponse(
            Long id,
            String firstName,
            String lastName,
            String level,
            String email,
            String phone,
            boolean isDirector,
            String aptitudes,
            String fonction,
            String licenseNumber,
            Long userId,
            java.time.LocalDate medicalCertDate,
            String comment,
            String club
    ) {
        public static PalanqueeMemberResponse from(PalanqueeMember m) {
            SlotDiver d = m.diver;
            String licenseNumber = d.licenseNumber;
            Long userId = null;
            if (d.email != null) {
                User user = User.findByEmail(d.email);
                if (user != null) {
                    if (licenseNumber == null) licenseNumber = user.licenseNumber;
                    userId = user.id;
                }
            }
            String apt = m.aptitudes != null ? m.aptitudes : d.aptitudes;
            String fct = m.fonction != null ? m.fonction : d.fonction;
            return new PalanqueeMemberResponse(d.id, d.firstName, d.lastName, d.level,
                    d.email, d.phone, d.isDirector, apt, fct, licenseNumber, userId,
                    d.medicalCertDate, d.comment, d.club);
        }
    }

    public record PalanqueeResponse(
            Long id,
            String name,
            int position,
            String depth,
            String duration,
            Long slotDiveId,
            List<PalanqueeMemberResponse> divers
    ) {
        public static PalanqueeResponse from(Palanquee p) {
            List<PalanqueeMemberResponse> divers = PalanqueeMember.findByPalanquee(p.id)
                    .stream().map(PalanqueeMemberResponse::from).toList();
            Long slotDiveId = p.slotDive != null ? p.slotDive.id : null;
            return new PalanqueeResponse(p.id, p.name, p.position, p.depth, p.duration, slotDiveId, divers);
        }
    }
}
