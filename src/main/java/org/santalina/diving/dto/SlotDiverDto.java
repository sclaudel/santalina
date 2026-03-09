package org.santalina.diving.dto;

import org.santalina.diving.domain.SlotDiver;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SlotDiverDto {

    public record SlotDiverRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String level,
            String email,
            String phone,
            boolean isDirector
    ) {}

    public record SlotDiverResponse(
            Long id,
            String firstName,
            String lastName,
            String level,
            String email,
            String phone,
            boolean isDirector
    ) {
        public static SlotDiverResponse from(SlotDiver d) {
            return new SlotDiverResponse(d.id, d.firstName, d.lastName, d.level,
                    d.email, d.phone, d.isDirector);
        }
    }
}
