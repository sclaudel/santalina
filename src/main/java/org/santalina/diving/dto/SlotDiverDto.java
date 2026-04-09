package org.santalina.diving.dto;

import org.santalina.diving.domain.RegistrationStatus;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SlotDiverDto {

    /** Requête d'ajout manuel d'un plongeur (ADMIN / DIVE_DIRECTOR) */
    public record SlotDiverRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String level,
            String email,
            String phone,
            boolean isDirector,
            String aptitudes,
            String licenseNumber,
            Integer numberOfDives,
            LocalDate lastDiveDate,
            String preparedLevel,
            String registrationComment
    ) {}

    /** Requête d'auto-inscription par un plongeur authentifié */
    public record SlotRegistrationRequest(
            @NotBlank String level,
            @NotNull @Min(0) Integer numberOfDives,
            @NotNull LocalDate lastDiveDate,
            @NotBlank @Email String email,
            String preparedLevel,
            String registrationComment
    ) {}

    /** Réponse standard pour un plongeur sur un créneau */
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
            RegistrationStatus registrationStatus,
            Integer numberOfDives,
            LocalDate lastDiveDate,
            String preparedLevel,
            String registrationComment,
            LocalDateTime registrationValidatedAt,
            LocalDateTime addedAt
    ) {
        public static SlotDiverResponse from(SlotDiver d) {
            String licenseNumber = d.licenseNumber;
            if (licenseNumber == null && d.email != null) {
                User user = User.findByEmail(d.email);
                if (user != null) licenseNumber = user.licenseNumber;
            }
            return new SlotDiverResponse(
                    d.id, d.firstName, d.lastName, d.level,
                    d.email, d.phone, d.isDirector, d.aptitudes, licenseNumber,
                    d.registrationStatus, d.numberOfDives, d.lastDiveDate,
                    d.preparedLevel, d.registrationComment, d.registrationValidatedAt,
                    d.addedAt);
        }
    }

    /** Vue de la file d'attente pour le DP (sans champs sensibles internes) */
    public record WaitlistEntryResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String level,
            Integer numberOfDives,
            LocalDate lastDiveDate,
            String preparedLevel,
            String registrationComment,
            LocalDateTime addedAt
    ) {
        public static WaitlistEntryResponse from(SlotDiver d) {
            return new WaitlistEntryResponse(
                    d.id, d.firstName, d.lastName, d.email, d.level,
                    d.numberOfDives, d.lastDiveDate, d.preparedLevel,
                    d.registrationComment, d.addedAt);
        }
    }
}
