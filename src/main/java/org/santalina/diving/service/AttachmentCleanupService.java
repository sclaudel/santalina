package org.santalina.diving.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.WaitingListEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Suppression automatique des pièces jointes (certificat médical, QR code de la licence)
 * liées aux inscriptions en liste d'attente, une semaine après la date du créneau.
 *
 * <p>Planifié chaque nuit à 02h00. Seules les entrées dont le créneau date
 * d'au moins 7 jours et dont les pièces jointes n'ont pas encore été supprimées
 * sont traitées.</p>
 */
@ApplicationScoped
public class AttachmentCleanupService {

    private static final Logger LOG = Logger.getLogger(AttachmentCleanupService.class);

    @Inject DivingConfig divingConfig;

    /**
     * Supprime les pièces jointes pour les créneaux dont la date est
     * antérieure d'au moins 7 jours à aujourd'hui.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldAttachments() {
        LocalDate cutoff = LocalDate.now().minusDays(7);

        List<WaitingListEntry> entries = WaitingListEntry.list(
                "slot.slotDate <= ?1 AND attachmentsDeletedAt IS NULL "
                        + "AND (medicalCertPath IS NOT NULL OR licenseQrPath IS NOT NULL)",
                cutoff);

        if (entries.isEmpty()) {
            LOG.debug("Aucune pièce jointe à nettoyer");
            return;
        }

        LOG.infof("Nettoyage des pièces jointes : %d entrée(s) concernée(s) (cutoff=%s)", entries.size(), cutoff);

        for (WaitingListEntry entry : entries) {
            deleteFileIfExists(entry.medicalCertPath);
            deleteFileIfExists(entry.licenseQrPath);

            // Supprimer le répertoire de l'entrée s'il est vide
            deleteEmptyEntryDir(entry);

            entry.medicalCertPath        = null;
            entry.licenseQrPath          = null;
            entry.attachmentsDeletedAt   = LocalDateTime.now();
            entry.persist();
        }

        LOG.infof("Nettoyage terminé : %d pièce(s) jointe(s) supprimée(s)", entries.size());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void deleteFileIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path p = Paths.get(divingConfig.dataDir()).resolve(relativePath);
            boolean deleted = Files.deleteIfExists(p);
            if (deleted) {
                LOG.debugf("Fichier supprimé : %s", p);
            }
        } catch (IOException e) {
            LOG.warnf("Impossible de supprimer %s : %s", relativePath, e.getMessage());
        }
    }

    private void deleteEmptyEntryDir(WaitingListEntry entry) {
        String anyPath = entry.medicalCertPath != null ? entry.medicalCertPath : entry.licenseQrPath;
        if (anyPath == null) return;
        try {
            Path entryDir = Paths.get(divingConfig.dataDir()).resolve(anyPath).getParent();
            if (entryDir == null) return;

            // Supprimer le répertoire de l'entrée ({entryId}/) s'il est vide
            deleteIfEmpty(entryDir);

            // Supprimer le répertoire du créneau ({slotId}/) s'il est vide
            Path slotDir = entryDir.getParent();
            if (slotDir != null) {
                deleteIfEmpty(slotDir);
            }
        } catch (IOException e) {
            LOG.warnf("Impossible de supprimer le répertoire vide : %s", e.getMessage());
        }
    }

    private void deleteIfEmpty(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.list(dir)) {
            if (stream.findAny().isEmpty()) {
                Files.delete(dir);
                LOG.debugf("Répertoire vide supprimé : %s", dir);
            }
        }
    }
}
