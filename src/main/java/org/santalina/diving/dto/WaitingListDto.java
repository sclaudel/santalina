package org.santalina.diving.dto;

import org.santalina.diving.domain.RegistrationStatus;
import org.santalina.diving.domain.WaitingListEntry;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WaitingListDto {

    /** Niveaux autorisés pour les plongeurs. */
    public static final String[] DIVER_LEVELS = {
        "N1", "N2", "N3", "N4", "N5",
        "E2", "E3", "E4",
        "PE12", "PE40", "PE60",
        "MF1", "MF2"
    };

    /** Niveaux en cours de préparation (champ facultatif). */
    public static final String[] PREPARED_LEVELS = {
        "Aucun",
        "N1", "N2", "N3", "N4", "N5",
        "MF1", "MF2",
        "E1", "E2", "E3", "E4",
        "PE12", "PE40", "PE60",
        "PA20", "PA40", "PA60",
        "PN", "PNC",
        "PB1", "PB2",
        "PV1", "PV2"
    };

    /**
     * Requête d'inscription en liste d'attente — soumise par un plongeur authentifié.
     */
    public record WaitingListRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotBlank String emailConfirm,
            @NotBlank String level,
            Integer numberOfDives,
            LocalDate lastDiveDate,
            String preparedLevel,
            String comment,
            LocalDate medicalCertDate,
            Boolean licenseConfirmed,
            String club
    ) {}

    /**
     * Réponse renvoyée au DP (liste complète) ou au plongeur (confirmation).
     */
    public record WaitingListResponse(
            Long id,
            Long slotId,
            String firstName,
            String lastName,
            String email,
            String level,
            Integer numberOfDives,
            LocalDate lastDiveDate,
            String preparedLevel,
            String comment,
            LocalDate medicalCertDate,
            boolean licenseConfirmed,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime registeredAt,
            String club,
            RegistrationStatus registrationStatus,
            String rejectionReason,
            boolean hasMedicalCert,
            boolean hasLicenseQr
    ) {
        public static WaitingListResponse from(WaitingListEntry e) {
            return new WaitingListResponse(
                    e.id,
                    e.slot.id,
                    e.firstName,
                    e.lastName,
                    e.email,
                    e.level,
                    e.numberOfDives,
                    e.lastDiveDate,
                    e.preparedLevel,
                    e.comment,
                    e.medicalCertDate,
                    e.licenseConfirmed,
                    e.registeredAt,
                    e.club,
                    e.registrationStatus,
                    e.rejectionReason,
                    e.medicalCertPath != null && e.attachmentsDeletedAt == null,
                    e.licenseQrPath   != null && e.attachmentsDeletedAt == null
            );
        }
    }

    /**
     * Requête de mise à jour du statut d'une inscription (DP / ADMIN uniquement).
     * Si {@code status} est {@code INCOMPLETE}, le champ {@code reason} est recommandé.
     */
    public record StatusUpdateRequest(
            @NotNull RegistrationStatus status,
            String reason
    ) {}

    /**
     * Requête de mise à jour des paramètres d'inscription du créneau (DP uniquement).
     */
    public record UpdateRegistrationRequest(
            boolean registrationOpen,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime registrationOpensAt,
            boolean requiresAttachments
    ) {}
}
