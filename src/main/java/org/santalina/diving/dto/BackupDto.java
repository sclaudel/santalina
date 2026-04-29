package org.santalina.diving.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.santalina.diving.domain.RegistrationStatus;

public class BackupDto {

    /** Format de l'archive de sauvegarde */
    public record BackupData(
            String version,
            String type,              // "config-users" | "full"
            LocalDateTime exportedAt,
            List<ConfigEntry> config,
            List<UserEntry> users,
            List<SlotEntry> slots,        // null si type="config-users"
            List<DiverEntry> divers,      // null si type="config-users"
            List<PalanqueeEntry> palanquees, // null si type="config-users"
            List<WaitingListBackupEntry> waitingListEntries, // null si type="config-users"
            List<SlotDiveEntry> slotDives // null si type="config-users"
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
            String club,
            boolean activated,
            boolean consentGiven,
            LocalDateTime consentDate,
            List<String> roles,
            boolean notifOnRegistration,
            boolean notifOnApproved,
            boolean notifOnCancelled,
            boolean notifOnMovedToWaitlist,
            boolean notifOnDpRegistration,
            boolean notifOnCreatorRegistration,
            boolean notifOnSafetyReminder,
            boolean clubCertified,
            String dpOrganizerEmailTemplate
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
            LocalDateTime createdAt,
            boolean registrationOpen,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime registrationOpensAt,
            boolean requiresAttachments
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
            String licenseNumber,
            Long palanqueeId,
            int palanqueePosition,
            LocalDate medicalCertDate,
            String comment,
            String club
    ) {}

    public record PalanqueeEntry(
            Long id,
            Long slotId,
            String name,
            int position,
            String depth,
            String duration,
            Long slotDiveId,
            List<Long> memberDiverIds  // IDs originaux des plongeurs, ordonnés par position
    ) {}

    public record SlotDiveEntry(
            Long id,
            Long slotId,
            int diveIndex,
            String label,
            LocalTime startTime,
            LocalTime endTime,
            String depth,
            String duration
    ) {}

    public record WaitingListBackupEntry(
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
            LocalDateTime registeredAt,
            LocalDate medicalCertDate,
            boolean licenseConfirmed,
            String club,
            RegistrationStatus registrationStatus,
            String rejectionReason
            // Note : medicalCertPath / licenseQrPath intentionnellement exclus
            // (les fichiers ne sont pas sauvegardés dans le JSON)
    ) {}

    /** Réponse d'un import */
    public record ImportResult(
            boolean success,
            String message,
            int configRestored,
            int usersRestored,
            int slotsRestored,
            int diversRestored,
            int palanqueesRestored,
            int waitingListRestored,
            int slotDivesRestored
    ) {}
}

