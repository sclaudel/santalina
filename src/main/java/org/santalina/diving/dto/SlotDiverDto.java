package org.santalina.diving.dto;

import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class SlotDiverDto {

    public record SlotDiverRequest(
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

    public record SlotDiverResponse(
            Long id,
            String firstName,
            String lastName,
            String level,
            String email,
            String phone,
            boolean isDirector,
            String aptitudes,
            String licenseNumber,
            Long userId,
            LocalDate medicalCertDate,
            String comment,
            String club
    ) {
        public static SlotDiverResponse from(SlotDiver d) {
            String licenseNumber = d.licenseNumber;
            Long userId = null;
            if (d.email != null) {
                User user = User.findByEmail(d.email);
                if (user != null) {
                    if (licenseNumber == null) licenseNumber = user.licenseNumber;
                    userId = user.id;
                }
            }
            return new SlotDiverResponse(d.id, d.firstName, d.lastName, d.level,
                    d.email, d.phone, d.isDirector, d.aptitudes, licenseNumber, userId,
                    d.medicalCertDate, d.comment, d.club);
        }

        /** Même chose mais avec les aptitudes surchargées (aptitudes spécifiques à une plongée). */
        public static SlotDiverResponse fromWithAptitudes(SlotDiver d, String aptitudesOverride) {
            String licenseNumber = d.licenseNumber;
            Long userId = null;
            if (d.email != null) {
                User user = User.findByEmail(d.email);
                if (user != null) {
                    if (licenseNumber == null) licenseNumber = user.licenseNumber;
                    userId = user.id;
                }
            }
            String apt = aptitudesOverride != null ? aptitudesOverride : d.aptitudes;
            return new SlotDiverResponse(d.id, d.firstName, d.lastName, d.level,
                    d.email, d.phone, d.isDirector, apt, licenseNumber, userId,
                    d.medicalCertDate, d.comment, d.club);
        }
    }
}
