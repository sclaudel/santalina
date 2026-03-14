package org.santalina.diving.dto;

import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SlotDiverDto {

    public record SlotDiverRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String level,
            String email,
            String phone,
            boolean isDirector,
            String aptitudes,
            String licenseNumber
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
            String licenseNumber
    ) {
        public static SlotDiverResponse from(SlotDiver d) {
            String licenseNumber = d.licenseNumber;
            if (licenseNumber == null && d.email != null) {
                User user = User.findByEmail(d.email);
                if (user != null) licenseNumber = user.licenseNumber;
            }
            return new SlotDiverResponse(d.id, d.firstName, d.lastName, d.level,
                    d.email, d.phone, d.isDirector, d.aptitudes, licenseNumber);
        }
    }
}
