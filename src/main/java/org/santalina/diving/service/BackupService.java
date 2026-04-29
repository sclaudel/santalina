package org.santalina.diving.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.*;
import org.santalina.diving.dto.BackupDto.*;
import org.santalina.diving.security.NameUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BackupService {

    private static final Logger LOG = Logger.getLogger(BackupService.class);
    private static final String BACKUP_VERSION = "1.0";

    @Inject
    EntityManager em;

    @Inject
    AuthService authService;

    // ---- Exports ----

    /** Export configuration + utilisateurs (sans créneaux). */
    public BackupData exportConfigUsers() {
        List<ConfigEntry> config = AppConfigEntry.<AppConfigEntry>listAll()
                .stream()
                .map(e -> new ConfigEntry(e.configKey, e.configValue))
                .collect(Collectors.toList());

        List<UserEntry> users = User.<User>listAll()
                .stream()
                .map(this::toUserEntry)
                .collect(Collectors.toList());

        LOG.infof("Export config-users : %d entrées de config, %d utilisateurs", config.size(), users.size());
        return new BackupData(BACKUP_VERSION, "config-users", LocalDateTime.now(),
                config, users, null, null, null, null);
    }

    /** Export complet (config + utilisateurs + créneaux + plongeurs). */
    public BackupData exportFull() {
        List<ConfigEntry> config = AppConfigEntry.<AppConfigEntry>listAll()
                .stream()
                .map(e -> new ConfigEntry(e.configKey, e.configValue))
                .collect(Collectors.toList());

        List<UserEntry> users = User.<User>listAll()
                .stream()
                .map(this::toUserEntry)
                .collect(Collectors.toList());

        List<SlotEntry> slots = DiveSlot.<DiveSlot>listAll()
                .stream()
                .map(this::toSlotEntry)
                .collect(Collectors.toList());

        List<DiverEntry> divers = SlotDiver.<SlotDiver>listAll()
                .stream()
                .map(this::toDiverEntry)
                .collect(Collectors.toList());

        List<PalanqueeEntry> palanquees = Palanquee.<Palanquee>listAll()
                .stream()
                .map(this::toPalanqueeEntry)
                .collect(Collectors.toList());

        List<WaitingListBackupEntry> waitingList = WaitingListEntry.<WaitingListEntry>listAll()
                .stream()
                .map(this::toWaitingListEntry)
                .collect(Collectors.toList());

        LOG.infof("Export full : %d config, %d users, %d slots, %d divers, %d palanquees, %d waitingList",
                config.size(), users.size(), slots.size(), divers.size(), palanquees.size(), waitingList.size());
        return new BackupData(BACKUP_VERSION, "full", LocalDateTime.now(),
                config, users, slots, divers, palanquees, waitingList);
    }

    // ---- Import ----

    /**
     * Importe une sauvegarde en vidant d'abord toutes les données existantes.
     * L'opération est atomique (tout dans une transaction).
     */
    @Transactional
    public ImportResult importBackup(BackupData backup) {
        LOG.warnf("Démarrage de l'import (type=%s, exportedAt=%s)", backup.type(), backup.exportedAt());

        // 1. Vider dans l'ordre des dépendances
        em.createNativeQuery("DELETE FROM waiting_list_entries").executeUpdate();
        em.createNativeQuery("DELETE FROM slot_divers").executeUpdate();
        em.createNativeQuery("DELETE FROM palanquees").executeUpdate();
        em.createNativeQuery("DELETE FROM dive_slots").executeUpdate();
        em.createNativeQuery("DELETE FROM user_roles").executeUpdate();
        em.createNativeQuery("DELETE FROM users").executeUpdate();
        em.createNativeQuery("DELETE FROM app_config").executeUpdate();

        // 2. Restaurer la configuration
        int configCount = 0;
        if (backup.config() != null) {
            for (ConfigEntry e : backup.config()) {
                AppConfigEntry entry = new AppConfigEntry();
                entry.configKey   = e.key();
                entry.configValue = e.value();
                entry.updatedAt   = LocalDateTime.now();
                entry.persist();
                configCount++;
            }
        }

        // 3. Restaurer les utilisateurs
        int userCount = 0;
        if (backup.users() != null) {
            for (UserEntry u : backup.users()) {
                User user = new User();
                user.email          = u.email() != null ? u.email().trim().toLowerCase() : null;
                user.passwordHash   = u.passwordHash();
                user.firstName      = NameUtil.capitalize(u.firstName());
                user.lastName       = u.lastName() != null ? u.lastName().trim().toUpperCase() : null;
                user.phone          = u.phone();
                user.licenseNumber  = u.licenseNumber();
                user.club           = u.club();
                user.activated      = u.activated();
                user.consentGiven   = u.consentGiven();
                user.consentDate    = u.consentDate();
                user.notifOnRegistration    = u.notifOnRegistration();
                user.notifOnApproved        = u.notifOnApproved();
                user.notifOnCancelled       = u.notifOnCancelled();
                user.notifOnMovedToWaitlist = u.notifOnMovedToWaitlist();
                user.notifOnDpRegistration  = u.notifOnDpRegistration();
                user.notifOnCreatorRegistration = u.notifOnCreatorRegistration();
                user.notifOnSafetyReminder  = u.notifOnSafetyReminder();
                user.clubCertified          = u.clubCertified();
                user.dpOrganizerEmailTemplate = u.dpOrganizerEmailTemplate();
                user.createdAt      = LocalDateTime.now();
                user.updatedAt      = LocalDateTime.now();
                // Rôles
                if (u.roles() != null) {
                    for (String roleName : u.roles()) {
                        try { user.roles.add(UserRole.valueOf(roleName)); } catch (Exception ignored) {}
                    }
                }
                user.role = user.roles.stream()
                        .filter(r -> r == UserRole.ADMIN).findFirst()
                        .orElse(user.roles.stream()
                                .filter(r -> r == UserRole.DIVE_DIRECTOR).findFirst()
                                .orElse(UserRole.DIVER));
                user.persist();
                userCount++;
            }
        }
        em.flush();

        // 4. Restaurer les créneaux (seulement si export full)
        int slotCount = 0;
        if (backup.slots() != null) {
            for (SlotEntry s : backup.slots()) {
                DiveSlot slot = new DiveSlot();
                slot.slotDate           = s.slotDate();
                slot.startTime          = s.startTime();
                slot.endTime            = s.endTime();
                slot.diverCount         = s.diverCount();
                slot.title              = s.title();
                slot.notes              = s.notes();
                slot.slotType           = s.slotType();
                slot.club               = s.club();
                slot.registrationOpen   = s.registrationOpen();
                slot.registrationOpensAt = s.registrationOpensAt();
                slot.requiresAttachments = s.requiresAttachments();
                slot.createdAt          = s.createdAt() != null ? s.createdAt() : LocalDateTime.now();
                slot.updatedAt          = LocalDateTime.now();
                // Lier au créateur si possible
                if (s.createdById() != null) {
                    User creator = User.find("email",
                            backup.users().stream()
                                    .filter(u -> u.id() != null && u.id().equals(s.createdById()))
                                    .map(UserEntry::email).findFirst().orElse(null))
                            .firstResult();
                    slot.createdBy = creator;
                }
                slot.persist();
                slotCount++;
            }
        }
        em.flush();

        // 5. Restaurer les plongeurs
        int diverCount = 0;
        if (backup.divers() != null) {
            for (DiverEntry d : backup.divers()) {
                // Retrouver le créneau correspondant par position dans la liste
                DiveSlot linkedSlot = null;
                if (d.slotId() != null && backup.slots() != null) {
                    // On cherche le slot par son slotDate+startTime de l'entrée originale
                    final Long origSlotId = d.slotId();
                    SlotEntry origSlot = backup.slots().stream()
                            .filter(s -> origSlotId.equals(s.id()))
                            .findFirst().orElse(null);
                    if (origSlot != null) {
                        linkedSlot = DiveSlot.find(
                                "slotDate = ?1 and startTime = ?2 and endTime = ?3",
                                origSlot.slotDate(), origSlot.startTime(), origSlot.endTime())
                                .firstResult();
                    }
                }
                if (linkedSlot == null) continue; // créneau introuvable → ignorer ce plongeur

                SlotDiver diver = new SlotDiver();
                diver.slot          = linkedSlot;
                diver.firstName     = NameUtil.capitalize(d.firstName());
                diver.lastName      = d.lastName() != null ? d.lastName().trim().toUpperCase() : null;
                diver.level         = d.level();
                diver.email         = d.email() != null ? d.email().trim().toLowerCase() : null;
                diver.phone         = d.phone();
                diver.isDirector    = d.isDirector();
                diver.aptitudes     = d.aptitudes();
                diver.licenseNumber = d.licenseNumber();
                diver.medicalCertDate = d.medicalCertDate();
                diver.comment       = d.comment();
                diver.club          = d.club();
                diver.addedAt       = LocalDateTime.now();
                diver.persist();
                diverCount++;
            }
        }

        // 6. Restaurer les palanquées et réassigner les plongeurs
        int palanqueeCount = 0;
        if (backup.palanquees() != null && backup.slots() != null) {
            for (PalanqueeEntry pe : backup.palanquees()) {
                // Retrouver le créneau correspondant
                final Long origSlotId = pe.slotId();
                SlotEntry origSlot = backup.slots().stream()
                        .filter(s -> origSlotId.equals(s.id()))
                        .findFirst().orElse(null);
                if (origSlot == null) continue;

                DiveSlot linkedSlot = DiveSlot.find(
                        "slotDate = ?1 and startTime = ?2 and endTime = ?3",
                        origSlot.slotDate(), origSlot.startTime(), origSlot.endTime())
                        .firstResult();
                if (linkedSlot == null) continue;

                Palanquee pal = new Palanquee();
                pal.slot     = linkedSlot;
                pal.name     = pe.name();
                pal.position = pe.position();
                pal.depth    = pe.depth();
                pal.duration = pe.duration();
                pal.persist();
                palanqueeCount++;

                // Réassigner les plongeurs de cette palanquée
                final Long origPalId = pe.id();
                if (backup.divers() != null) {
                    for (DiverEntry de : backup.divers()) {
                        if (origPalId.equals(de.palanqueeId())) {
                            // Retrouver le plongeur restauré par slot + nom
                            SlotDiver sd = SlotDiver.find(
                                    "slot = ?1 and firstName = ?2 and lastName = ?3",
                                    linkedSlot, de.firstName(), de.lastName())
                                    .firstResult();
                            if (sd != null) {
                                sd.palanquee = pal;
                                sd.palanqueePosition = de.palanqueePosition();
                            }
                        }
                    }
                }
            }
        }
        em.flush();

        // 7. Restaurer les entrées en liste d'attente
        int waitingListCount = 0;
        if (backup.waitingListEntries() != null && backup.slots() != null) {
            for (WaitingListBackupEntry we : backup.waitingListEntries()) {
                final Long origSlotId = we.slotId();
                SlotEntry origSlot = backup.slots().stream()
                        .filter(s -> origSlotId.equals(s.id()))
                        .findFirst().orElse(null);
                if (origSlot == null) continue;

                DiveSlot linkedSlot = DiveSlot.find(
                        "slotDate = ?1 and startTime = ?2 and endTime = ?3",
                        origSlot.slotDate(), origSlot.startTime(), origSlot.endTime())
                        .firstResult();
                if (linkedSlot == null) continue;

                WaitingListEntry entry = new WaitingListEntry();
                entry.slot          = linkedSlot;
                entry.firstName     = we.firstName();
                entry.lastName      = we.lastName();
                entry.email         = we.email();
                entry.level         = we.level();
                entry.numberOfDives = we.numberOfDives();
                entry.lastDiveDate  = we.lastDiveDate();
                entry.preparedLevel = we.preparedLevel();
                entry.comment       = we.comment();
                entry.registeredAt    = we.registeredAt() != null ? we.registeredAt() : LocalDateTime.now();
                entry.medicalCertDate = we.medicalCertDate();
                entry.licenseConfirmed = we.licenseConfirmed();
                entry.club          = we.club();
                entry.registrationStatus = we.registrationStatus() != null
                        ? we.registrationStatus()
                        : org.santalina.diving.domain.RegistrationStatus.PENDING_VERIFICATION;
                entry.rejectionReason = we.rejectionReason();
                // medicalCertPath / licenseQrPath non restaurés (fichiers absents après restore)
                entry.persist();
                waitingListCount++;
            }
        }
        em.flush();

        // 8. S'assurer qu'un admin existe toujours (au cas où le backup importé
        //    ne contenait aucun utilisateur admin).
        authService.ensureAdminExists();

        LOG.infof("Import terminé : %d config, %d users, %d slots, %d divers, %d palanquees, %d waitingList",
                configCount, userCount, slotCount, diverCount, palanqueeCount, waitingListCount);

        return new ImportResult(true,
                String.format("Import réussi : %d config, %d utilisateurs, %d créneaux, %d plongeurs, %d palanquées, %d liste d'attente",
                        configCount, userCount, slotCount, diverCount, palanqueeCount, waitingListCount),
                configCount, userCount, slotCount, diverCount, palanqueeCount, waitingListCount);
    }

    // ---- Mappeurs ----

    private UserEntry toUserEntry(User u) {
        List<String> roles = u.roles.stream().map(Enum::name).collect(Collectors.toList());
        return new UserEntry(u.id, u.email, u.passwordHash,
                u.firstName, u.lastName, u.phone, u.licenseNumber, u.club,
                u.activated, u.consentGiven, u.consentDate, roles,
                u.notifOnRegistration, u.notifOnApproved, u.notifOnCancelled,
                u.notifOnMovedToWaitlist, u.notifOnDpRegistration, u.notifOnCreatorRegistration,
                u.notifOnSafetyReminder, u.clubCertified, u.dpOrganizerEmailTemplate);
    }

    private SlotEntry toSlotEntry(DiveSlot s) {
        return new SlotEntry(s.id, s.slotDate, s.startTime, s.endTime,
                s.diverCount, s.title, s.notes, s.slotType, s.club,
                s.createdBy != null ? s.createdBy.id : null, s.createdAt,
                s.registrationOpen, s.registrationOpensAt, s.requiresAttachments);
    }

    private DiverEntry toDiverEntry(SlotDiver d) {
        return new DiverEntry(d.id, d.slot != null ? d.slot.id : null,
                d.firstName, d.lastName, d.level, d.email, d.phone,
                d.isDirector, d.aptitudes, d.licenseNumber,
                d.palanquee != null ? d.palanquee.id : null, d.palanqueePosition,
                d.medicalCertDate, d.comment, d.club);
    }

    private PalanqueeEntry toPalanqueeEntry(Palanquee p) {
        return new PalanqueeEntry(p.id, p.slot != null ? p.slot.id : null,
                p.name, p.position, p.depth, p.duration);
    }

    private WaitingListBackupEntry toWaitingListEntry(WaitingListEntry e) {
        return new WaitingListBackupEntry(e.id, e.slot != null ? e.slot.id : null,
                e.firstName, e.lastName, e.email, e.level,
                e.numberOfDives, e.lastDiveDate, e.preparedLevel, e.comment, e.registeredAt,
                e.medicalCertDate, e.licenseConfirmed, e.club,
                e.registrationStatus, e.rejectionReason);
        // medicalCertPath / licenseQrPath exclus intentionnellement
    }
}



