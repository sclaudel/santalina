package org.santalina.diving.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class BackupDto {

    /** Format de l'archive de sauvegarde */
    public record BackupData(
            String version,
            String type,              // "config-users" | "full"
            LocalDateTime exportedAt,
            List<ConfigEntry> config,
            List<UserEntry> users,
            List<SlotEntry> slots,    // null si type="config-users"
            List<DiverEntry> divers   // null si type="config-users"
    ) {}

    public record ConfigEntry(
            String key,
            String value
    ) {}

    public record UserEntry(
            Long id,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String phone,
            String licenseNumber,
            boolean activated,
            boolean consentGiven,
            LocalDateTime consentDate,
            List<String> roles
    ) {}

    public record SlotEntry(
            Long id,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime,
            int diverCount,
            String title,
            String notes,
            String slotType,
            String club,
            Long createdById,
            LocalDateTime createdAt
    ) {}

    public record DiverEntry(
            Long id,
            Long slotId,
            String firstName,
            String lastName,
            String level,
            String email,
            String phone,
            boolean isDirector,
            String aptitudes,
            String licenseNumber
    ) {}

    /** Réponse d'un import */
    public record ImportResult(
            boolean success,
            String message,
            int configRestored,
            int usersRestored,
            int slotsRestored,
            int diversRestored
    ) {}
}

