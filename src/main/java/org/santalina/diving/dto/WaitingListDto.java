package org.santalina.diving.dto;

import org.santalina.diving.domain.WaitingListEntry;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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
            String comment
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
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime registeredAt
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
                    e.registeredAt
            );
        }
    }

    /**
     * Requête de mise à jour des paramètres d'inscription du créneau (DP uniquement).
     */
    public record UpdateRegistrationRequest(
            boolean registrationOpen,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime registrationOpensAt
    ) {}
}
