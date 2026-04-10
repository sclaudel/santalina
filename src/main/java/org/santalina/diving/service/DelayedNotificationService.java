package org.santalina.diving.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.WaitingListEntry;
import org.santalina.diving.mail.WaitingListMailer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Notifications e-mail différées avec un délai de grâce de 15 minutes.
 * Cela permet au DP de corriger une erreur (ex : remise en liste d'attente)
 * avant que le mail de confirmation ne soit envoyé au plongeur.
 */
@ApplicationScoped
public class DelayedNotificationService {

    private static final Logger LOG = Logger.getLogger(DelayedNotificationService.class);

    /** Délai de grâce avant envoi du mail (en minutes). */
    static final long DELAY_MINUTES = 15;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Inject
    WaitingListMailer mailer;

    // -------------------------------------------------------------------------
    // Inscription validée → mail au plongeur (délai de grâce)
    // -------------------------------------------------------------------------

    /**
     * Planifie l'envoi du mail "inscription validée" après le délai de grâce.
     * Avant l'envoi, vérifie que le plongeur est toujours dans slot_divers.
     * Si le DP l'a remis en liste d'attente entre-temps, le mail n'est pas envoyé.
     *
     * @param slotId    identifiant du créneau
     * @param email     e-mail du plongeur
     * @param firstName prénom
     * @param lastName  nom
     */
    public void scheduleApprovedMail(Long slotId, String email,
                                     String firstName, String lastName) {
        scheduler.schedule(() -> {
            try {
                QuarkusTransaction.requiringNew().run(() -> {
                    // Vérifier que le plongeur est toujours inscrit (pas re-déplacé en L.A.)
                    SlotDiver diver = SlotDiver.findBySlotAndEmail(slotId, email);
                    if (diver == null) {
                        LOG.infof("[NOTIF DIFFÉRÉE ANNULÉE][approved] Le plongeur %s n'est plus dans slot_divers pour le créneau %d", email, slotId);
                        return;
                    }
                    DiveSlot slot = DiveSlot.findById(slotId);
                    if (slot == null) return;
                    mailer.sendRegistrationApproved(email, firstName, lastName, slot);
                });
            } catch (Exception e) {
                LOG.warnf("Erreur lors de l'envoi différé du mail d'approbation pour %s : %s", email, e.getMessage());
            }
        }, DELAY_MINUTES, TimeUnit.MINUTES);
    }

    // -------------------------------------------------------------------------
    // Remis en liste d'attente → mail au plongeur (délai de grâce)
    // -------------------------------------------------------------------------

    /**
     * Planifie l'envoi du mail "remis en liste d'attente" après le délai de grâce.
     * Avant l'envoi, vérifie que l'entrée est toujours présente en liste d'attente.
     * Si le plongeur a annulé ou a été ré-approuvé, le mail n'est pas envoyé.
     *
     * @param entryId   identifiant de l'entrée en liste d'attente
     * @param slotId    identifiant du créneau
     * @param email     e-mail du plongeur
     * @param firstName prénom
     * @param lastName  nom
     * @param level     niveau de plongée
     */
    public void scheduleMovedToWlMail(Long entryId, Long slotId,
                                      String email, String firstName,
                                      String lastName, String level) {
        scheduler.schedule(() -> {
            try {
                QuarkusTransaction.requiringNew().run(() -> {
                    WaitingListEntry entry = WaitingListEntry.findById(entryId);
                    if (entry == null) {
                        LOG.infof("[NOTIF DIFFÉRÉE ANNULÉE][moved_to_wl] L'entrée %d n'est plus en liste d'attente", entryId);
                        return;
                    }
                    DiveSlot slot = DiveSlot.findById(slotId);
                    if (slot == null) return;
                    mailer.sendMovedToWaitlist(email, firstName, lastName, level, slot);
                });
            } catch (Exception e) {
                LOG.warnf("Erreur lors de l'envoi différé du mail 'remis en L.A.' pour %s : %s", email, e.getMessage());
            }
        }, DELAY_MINUTES, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
