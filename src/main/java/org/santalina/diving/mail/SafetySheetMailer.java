package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.service.ConfigService;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Envoie une notification par e-mail lorsque des fiches de sécurité
 * sont déposées sur un créneau.
 */
@ApplicationScoped
public class SafetySheetMailer {

    private static final Logger LOG = Logger.getLogger(SafetySheetMailer.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject Mailer        mailer;
    @Inject ConfigService configService;

    /**
     * Envoie le mail de notification aux destinataires configurés.
     *
     * @param slot créneau sur lequel des fiches ont été déposées
     */
    public void sendNotification(DiveSlot slot) {
        String recipientsCsv = configService.getSafetySheetNotificationEmails();
        if (recipientsCsv == null || recipientsCsv.isBlank()) {
            LOG.debug("[SafetySheet] Aucun destinataire de notification configuré, envoi ignoré.");
            return;
        }

        List<String> recipients = Arrays.stream(recipientsCsv.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (recipients.isEmpty()) return;

        String siteName = configService.getSiteName();
        String dateStr  = slot.slotDate.format(DATE_FMT);
        String subject  = "[" + siteName + "] Fiches de sécurité déposées — créneau du " + dateStr;
        String body     = buildBody(siteName, slot, dateStr);

        for (String recipient : recipients) {
            try {
                mailer.send(Mail.withHtml(recipient, subject, body));
                LOG.infof("[SafetySheet] Notification envoyée à %s pour le créneau #%d", recipient, slot.id);
            } catch (Exception e) {
                LOG.errorf(e, "[SafetySheet] Erreur lors de l'envoi à %s", recipient);
            }
        }
    }

    private String buildBody(String siteName, DiveSlot slot, String dateStr) {
        String title = slot.title != null && !slot.title.isBlank() ? slot.title : "sans titre";
        String hours = slot.startTime + " – " + slot.endTime;
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#1e40af;">Fiches de sécurité déposées</h2>
                  <p>Bonjour,</p>
                  <p>
                    Des fiches de sécurité ont été déposées sur le site <strong>%s</strong>
                    pour le créneau suivant :
                  </p>
                  <table style="border-collapse:collapse;width:100%%;margin:12px 0;">
                    <tr><td style="padding:6px 12px;background:#f0f9ff;font-weight:bold;">Date</td>
                        <td style="padding:6px 12px;">%s</td></tr>
                    <tr><td style="padding:6px 12px;background:#f0f9ff;font-weight:bold;">Horaires</td>
                        <td style="padding:6px 12px;">%s</td></tr>
                    <tr><td style="padding:6px 12px;background:#f0f9ff;font-weight:bold;">Intitulé</td>
                        <td style="padding:6px 12px;">%s</td></tr>
                  </table>
                  <p>Connectez-vous sur l'application pour consulter et télécharger les fichiers.</p>
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;" />
                  <p style="color:#6b7280;font-size:12px;">Système de réservation — %s</p>
                </body>
                </html>
                """.formatted(siteName, dateStr, hours, title, siteName);
    }
}
