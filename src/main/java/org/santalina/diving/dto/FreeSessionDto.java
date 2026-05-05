package org.santalina.diving.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.santalina.diving.domain.FreeDiveSession;
import org.santalina.diving.domain.FreePalanquee;
import org.santalina.diving.domain.FreePalanqueeMember;
import org.santalina.diving.domain.FreeSessionDive;
import org.santalina.diving.domain.FreeSessionDiver;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class FreeSessionDto {

    // ── Session ──────────────────────────────────────────────────────────────

    public record CreateSessionRequest(
            String label,
            @NotNull LocalDate diveDate,
            @NotNull LocalTime startTime,
            String notes
    ) {}

    public record UpdateSessionRequest(
            String label,
            @NotNull LocalDate diveDate,
            @NotNull LocalTime startTime,
            String notes
    ) {}

    public record SessionResponse(
            Long id,
            String label,
            LocalDate diveDate,
            LocalTime startTime,
            String notes,
            int diverCount,
            int palanqueeCount
    ) {
        public static SessionResponse from(FreeDiveSession s) {
            int diverCount    = (int) FreeSessionDiver.count("session.id = ?1", s.id);
            int palanqueeCount = (int) FreePalanquee.count("session.id = ?1", s.id);
            return new SessionResponse(s.id, s.label, s.diveDate, s.startTime, s.notes,
                    diverCount, palanqueeCount);
        }
    }

    // ── Divers ───────────────────────────────────────────────────────────────

    public record CreateDiverRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String level,
            String email,
            String phone,
            boolean isDirector,
            String aptitudes,
            String licenseNumber,
            String club
    ) {}

    public record UpdateDiverRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String level,
            String email,
            String phone,
            boolean isDirector,
            String aptitudes,
            String licenseNumber,
            String club
    ) {}

    public record DiverResponse(
            Long id,
            String firstName,
            String lastName,
            String level,
            String email,
            String phone,
            boolean isDirector,
            String aptitudes,
            String licenseNumber,
            LocalDate medicalCertDate,
            String comment,
            String club
    ) {
        public static DiverResponse from(FreeSessionDiver d) {
            return new DiverResponse(d.id, d.firstName, d.lastName, d.level,
                    d.email, d.phone, d.isDirector, d.aptitudes, d.licenseNumber,
                    d.medicalCertDate, d.comment, d.club);
        }

        public static DiverResponse fromWithAptitudes(FreeSessionDiver d, String aptitudesOverride) {
            String apt = aptitudesOverride != null ? aptitudesOverride : d.aptitudes;
            return new DiverResponse(d.id, d.firstName, d.lastName, d.level,
                    d.email, d.phone, d.isDirector, apt, d.licenseNumber,
                    d.medicalCertDate, d.comment, d.club);
        }
    }

    // ── Dives ────────────────────────────────────────────────────────────────

    public record CreateDiveRequest(
            String label,
            LocalTime startTime,
            LocalTime endTime,
            String depth,
            String duration
    ) {}

    public record UpdateDiveRequest(
            String label,
            LocalTime startTime,
            LocalTime endTime,
            String depth,
            String duration
    ) {}

    public record DiveResponse(
            Long id,
            int diveIndex,
            String label,
            LocalTime startTime,
            LocalTime endTime,
            String depth,
            String duration
    ) {
        public static DiveResponse from(FreeSessionDive d) {
            return new DiveResponse(d.id, d.diveIndex, d.label,
                    d.startTime, d.endTime, d.depth, d.duration);
        }
    }

    /** Assigne ou désassigne une palanquée libre à une plongée libre */
    public record AssignPalanqueeToDiveRequest(
            @NotNull Long palanqueeId,
            Long diveId
    ) {}

    // ── Palanquées ───────────────────────────────────────────────────────────

    public record CreatePalanqueeRequest(
            @NotBlank String name
    ) {}

    public record UpdatePalanqueeRequest(
            @NotBlank String name,
            String depth,
            String duration
    ) {}

    /** diverId obligatoire ; palanqueeId nullable = désassigner */
    public record AssignDiverRequest(
            @NotNull Long diverId,
            Long palanqueeId,
            Long fromPalanqueeId
    ) {}

    public record ReorderRequest(
            @NotNull List<Long> diverIds
    ) {}

    public record UpdateMemberAptitudesRequest(String aptitudes) {}

    public record PalanqueeResponse(
            Long id,
            String name,
            int position,
            String depth,
            String duration,
            Long diveId,
            List<DiverResponse> divers
    ) {
        public static PalanqueeResponse from(FreePalanquee p) {
            List<DiverResponse> divers = FreePalanqueeMember.findByPalanquee(p.id)
                    .stream()
                    .map(m -> DiverResponse.fromWithAptitudes(m.diver, m.aptitudes))
                    .toList();
            Long diveId = p.dive != null ? p.dive.id : null;
            return new PalanqueeResponse(p.id, p.name, p.position, p.depth, p.duration, diveId, divers);
        }
    }
}
