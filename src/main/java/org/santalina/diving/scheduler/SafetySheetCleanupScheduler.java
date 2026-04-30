package org.santalina.diving.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.SlotSafetySheet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Suppression automatique des fiches de sécurité expirées (conservation 1 an).
 * Planifié chaque nuit à 03h00.
 */
@ApplicationScoped
public class SafetySheetCleanupScheduler {

    private static final Logger LOG = Logger.getLogger(SafetySheetCleanupScheduler.class);

    @Inject DivingConfig divingConfig;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredSheets() {
        LocalDateTime now = LocalDateTime.now();
        List<SlotSafetySheet> expired = SlotSafetySheet.findExpired(now);

        if (expired.isEmpty()) {
            LOG.debug("[SafetySheetCleanup] Aucune fiche de sécurité expirée.");
            return;
        }

        LOG.infof("[SafetySheetCleanup] %d fiche(s) expirée(s) à supprimer.", expired.size());

        for (SlotSafetySheet sheet : expired) {
            deleteFile(sheet.filePath);
            sheet.delete();
        }

        LOG.infof("[SafetySheetCleanup] Suppression terminée.");
    }

    private void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path file = Paths.get(divingConfig.dataDir()).resolve(relativePath);
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                LOG.debugf("[SafetySheetCleanup] Fichier supprimé : %s", file);
            }
            // Supprimer le répertoire parent s'il est vide
            Path parent = file.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                try (var stream = Files.list(parent)) {
                    if (stream.findAny().isEmpty()) Files.delete(parent);
                }
            }
        } catch (IOException e) {
            LOG.warnf("[SafetySheetCleanup] Impossible de supprimer %s : %s", relativePath, e.getMessage());
        }
    }
}
