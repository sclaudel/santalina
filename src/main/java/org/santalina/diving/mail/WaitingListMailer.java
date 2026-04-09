package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.WaitingListEntry;
import org.santalina.diving.service.ConfigService;

/**
 * Notifications e-mail relatives à la liste d'attente :
 * <ul>
 *   <li>Confirmation d'inscription en liste d'attente → plongeur</li>
 *   <li>Validation de l'inscription (transfert vers le pool) → plongeur</li>
 *   <li>Annulation de participation → DP du créneau</li>
 * </ul>
 */
@ApplicationScoped
public class WaitingListMailer {

    private static final Logger LOG = Logger.getLogger(WaitingListMailer.class);

    @Inject
    Mailer mailer;

    @Inject
    ConfigService configService;

    // -------------------------------------------------------------------------
    // Inscription en liste d'attente → mail au plongeur
    // -------------------------------------------------------------------------

    public void sendWaitingListConfirmation(WaitingListEntry entry, DiveSlot slot) {
        if (entry.email == null || entry.email.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Inscription en liste d'attente — " + slotLabel;

        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#1e40af;">🤿 Inscription en liste d'attente</h2>
                  <p>Bonjour <strong>%s %s</strong>,</p>
                  <p>Votre demande d'inscription pour le créneau suivant a bien été enregistrée.
                     Le directeur de plongée examinera votre demande et vous informera de sa décision.</p>
                  <table style="border-collapse:collapse;width:100%%;margin:16px 0;">
                    <tr><td style="padding:4px 8px;color:#6b7280;">Créneau :</td>
                        <td style="padding:4px 8px;"><strong>%s</strong></td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">Date :</td>
                        <td style="padding:4px 8px;">%s</td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">Horaire :</td>
                        <td style="padding:4px 8px;">%s – %s</td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">Votre niveau :</td>
                        <td style="padding:4px 8px;">%s</td></tr>
                  </table>
                  <p style="color:#6b7280;font-size:13px;">
                    Vous pouvez annuler votre inscription depuis la page du créneau tant que votre place n'a pas encore été validée.
                  </p>
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;"/>
                  <p style="color:#6b7280;font-size:12px;">Système de réservation — %s</p>
                </body>
                </html>
                """.formatted(
                entry.firstName, entry.lastName,
                slotLabel, slot.slotDate, slot.startTime, slot.endTime,
                entry.level,
                siteName
        );

        sendSingle(entry.email, subject, body);
    }

    // -------------------------------------------------------------------------
    // Validation → mail au plongeur
    // -------------------------------------------------------------------------

    public void sendRegistrationApproved(WaitingListEntry entry, DiveSlot slot) {
        if (entry.email == null || entry.email.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Inscription validée — " + slotLabel;

        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#16a34a;">✅ Inscription validée !</h2>
                  <p>Bonjour <strong>%s %s</strong>,</p>
                  <p>Votre inscription pour le créneau suivant a été <strong>validée</strong> par le directeur de plongée.
                     Vous faites maintenant partie des plongeurs confirmés pour cette sortie.</p>
                  <table style="border-collapse:collapse;width:100%%;margin:16px 0;">
                    <tr><td style="padding:4px 8px;color:#6b7280;">Créneau :</td>
                        <td style="padding:4px 8px;"><strong>%s</strong></td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">Date :</td>
                        <td style="padding:4px 8px;">%s</td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">Horaire :</td>
                        <td style="padding:4px 8px;">%s – %s</td></tr>
                  </table>
                  <p>À très bientôt sous l'eau ! 🌊</p>
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;"/>
                  <p style="color:#6b7280;font-size:12px;">Système de réservation — %s</p>
                </body>
                </html>
                """.formatted(
                entry.firstName, entry.lastName,
                slotLabel, slot.slotDate, slot.startTime, slot.endTime,
                siteName
        );

        sendSingle(entry.email, subject, body);
    }

    // -------------------------------------------------------------------------
    // Annulation → mail au DP
    // -------------------------------------------------------------------------

    public void sendCancellationToDP(WaitingListEntry entry, DiveSlot slot, String dpEmail) {
        if (dpEmail == null || dpEmail.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Annulation d'inscription — " + slotLabel;

        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#dc2626;">❌ Annulation d'inscription</h2>
                  <p>Le/la plongeur(se) <strong>%s %s</strong> a annulé sa demande d'inscription
                     pour le créneau suivant :</p>
                  <table style="border-collapse:collapse;width:100%%;margin:16px 0;">
                    <tr><td style="padding:4px 8px;color:#6b7280;">Créneau :</td>
                        <td style="padding:4px 8px;"><strong>%s</strong></td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">Date :</td>
                        <td style="padding:4px 8px;">%s</td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">Horaire :</td>
                        <td style="padding:4px 8px;">%s – %s</td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">E-mail :</td>
                        <td style="padding:4px 8px;">%s</td></tr>
                    <tr><td style="padding:4px 8px;color:#6b7280;">Niveau :</td>
                        <td style="padding:4px 8px;">%s</td></tr>
                  </table>
                  <p style="color:#6b7280;font-size:13px;">Pensez à vérifier l'organisation de la sortie.</p>
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;"/>
                  <p style="color:#6b7280;font-size:12px;">Système de réservation — %s</p>
                </body>
                </html>
                """.formatted(
                entry.firstName, entry.lastName,
                slotLabel, slot.slotDate, slot.startTime, slot.endTime,
                entry.email, entry.level,
                siteName
        );

        sendSingle(dpEmail, subject, body);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String slotLabel(DiveSlot slot) {
        return (slot.title != null && !slot.title.isBlank())
                ? slot.title
                : "Créneau du " + slot.slotDate;
    }

    private void sendSingle(String to, String subject, String body) {
        try {
            mailer.send(Mail.withHtml(to, subject, body));
        } catch (Exception e) {
            LOG.warnf("Impossible d'envoyer le mail '%s' à %s : %s", subject, to, e.getMessage());
        }
    }
}
