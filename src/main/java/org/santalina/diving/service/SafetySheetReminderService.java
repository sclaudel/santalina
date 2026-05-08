package org.santalina.diving.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.SlotSafetySheet;
import org.santalina.diving.mail.WaitingListMailer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service planifié : envoie chaque matin un rappel de fiche de sécurité
 * au DP assigné sur les créneaux passés depuis N jours (configurable).
 * Activée/désactivée via la config {@code notif.safety_reminder.enabled}.
 * 
 * Le rappel est envoyé dès que le nombre de jours réels écoulés depuis le créneau
 * atteint ou dépasse la valeur configurée (indépendant des redémarrages serveur).
 */
@ApplicationScoped
public class SafetySheetReminderService {

    private static final Logger LOG = Logger.getLogger(SafetySheetReminderService.class);

    @Inject
    ConfigService configService;

    @Inject
    WaitingListMailer mailer;

    /** Exécuté tous les jours à 08h00. */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendPendingReminders() {
        if (!configService.isNotifSafetyReminderEnabled()) {
            LOG.debug("[SafetyReminder] Désactivé globalement, aucun envoi.");
            return;
        }

        int delayDays = configService.getSafetyReminderDelayDays();
        LocalDate activationDate = configService.getSafetyReminderActivationDate();
        LocalDate today = LocalDate.now();

        // Récupérer tous les créneaux pour lesquels le rappel n'a pas encore été envoyé
        List<DiveSlot> allSlots = DiveSlot.list("reminderSentAt IS NULL ORDER BY slotDate");

        int sent = 0;
        int skipped = 0;
        for (DiveSlot slot : allSlots) {
            // Vérifier la date d'activation si définie
            if (activationDate != null && slot.slotDate.isBefore(activationDate)) {
                continue;
            }

            // Calculer le nombre de jours réels écoulés depuis le créneau
            long daysSinceSlot = ChronoUnit.DAYS.between(slot.slotDate, today);

            // Envoyer le rappel si le délai est atteint
            if (daysSinceSlot < delayDays) {
                continue;
            }

            // Vérifier si une fiche de sécurité a déjà été déposée
            if (SlotSafetySheet.countBySlot(slot.id) > 0) {
                LOG.debugf("[SafetyReminder] Fiche de sécurité déjà déposée pour le créneau %d (%s), rappel annulé", slot.id, slot.slotDate);
                slot.reminderSentAt = LocalDateTime.now();
                slot.persist();
                skipped++;
                continue;
            }

            List<SlotDiver> directors = SlotDiver.list(
                    "slot = ?1 AND isDirector = true", slot);

            if (directors.isEmpty()) {
                // Pas de DP : marquer quand même pour ne pas réessayer indéfiniment
                slot.reminderSentAt = LocalDateTime.now();
                slot.persist();
                skipped++;
                continue;
            }

            SlotDiver dp = directors.get(0);
            if (dp.email == null || dp.email.isBlank()) {
                slot.reminderSentAt = LocalDateTime.now();
                slot.persist();
                skipped++;
                continue;
            }

            mailer.sendSafetySheetReminder(dp.email, dp.firstName, dp.lastName, slot);
            slot.reminderSentAt = LocalDateTime.now();
            slot.persist();
            sent++;
        }

        if (sent > 0 || skipped > 0) {
            LOG.infof("[SafetyReminder] %d rappel(s) envoyé(s), %d créneau(x) ignoré(s) (sans DP/email) — délai configuré : %d jours",
                    sent, skipped, delayDays);
        }
    }
}
