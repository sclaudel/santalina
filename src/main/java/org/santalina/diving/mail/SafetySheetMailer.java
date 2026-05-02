package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotSafetySheet;
import org.santalina.diving.service.ConfigService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Envoie une notification par e-mail lorsque des fiches de sécurité
 * sont déposées sur un créneau.
 */
@ApplicationScoped
public class SafetySheetMailer {

    private static final Logger LOG = Logger.getLogger(SafetySheetMailer.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject ReactiveMailer reactiveMailer;
    @Inject ConfigService  configService;
    @Inject DivingConfig   divingConfig;

    /**
     * Envoie le mail de notification aux destinataires configurés
     * ainsi qu'aux adresses supplémentaires spécifiées lors du dépôt,
     * avec les fiches de sécurité en pièces jointes.
     *
     * @param slot             créneau sur lequel des fiches ont été déposées
     * @param sheets           fichiers nouvellement déposés à joindre au mail
     * @param isFollowUp       {@code true} si des fiches existaient déjà (réenvoi)
     * @param additionalEmails adresses email supplémentaires saisies par le DP (CSV)
     */
    public void sendNotification(DiveSlot slot, List<SlotSafetySheet> sheets,
                                 boolean isFollowUp, String additionalEmails) {
        String recipientsCsv = configService.getSafetySheetNotificationEmails();
        String siteName = configService.getSiteName();
        sendNotification(slot, sheets, isFollowUp, additionalEmails, recipientsCsv, siteName);
        }

        private void sendNotification(DiveSlot slot, List<SlotSafetySheet> sheets,
                      boolean isFollowUp, String additionalEmails,
                      String recipientsCsv, String siteName) {
        List<String> configuredRecipients = new java.util.LinkedHashSet<>(
                Arrays.stream(recipientsCsv != null ? recipientsCsv.split("[,;]") : new String[0])
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList())
        ).stream().collect(Collectors.toList());

        List<String> additionalRecipients = new java.util.ArrayList<>();
        if (additionalEmails != null && !additionalEmails.isBlank()) {
            Arrays.stream(additionalEmails.split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .forEach(e -> {
                        boolean alreadyInTo = configuredRecipients.stream().anyMatch(c -> c.equalsIgnoreCase(e));
                        boolean alreadyInCc = additionalRecipients.stream().anyMatch(c -> c.equalsIgnoreCase(e));
                        if (!alreadyInTo && !alreadyInCc) {
                            additionalRecipients.add(e);
                        }
                    });
        }

        if (configuredRecipients.isEmpty() && additionalRecipients.isEmpty()) {
            LOG.debug("[SafetySheet] Aucun destinataire, envoi ignoré.");
            return;
        }

        String dateStr  = slot.slotDate.format(DATE_FMT);
        String followUpLabel = isFollowUp ? " (complément)" : "";
        String subject  = "[" + siteName + "] Fiches de sécurité déposées" + followUpLabel + " — créneau du " + dateStr;
        String body     = buildBody(siteName, slot, dateStr, sheets.size(), isFollowUp);

        try {
            // To = destinataires configurés ; Cc = destinataires supplémentaires saisis lors du dépôt
            List<String> toRecipients = configuredRecipients.isEmpty() ? additionalRecipients : configuredRecipients;
            List<String> ccRecipients = configuredRecipients.isEmpty() ? List.of() : additionalRecipients;

            Mail mail = Mail.withHtml(toRecipients.get(0), subject, body);
            if (toRecipients.size() > 1) {
                mail.setTo(toRecipients);
            }
            if (!ccRecipients.isEmpty()) {
                mail.setCc(ccRecipients);
            }

            // Joindre chaque fiche au mail
            for (SlotSafetySheet sheet : sheets) {
                java.nio.file.Path path = Paths.get(divingConfig.dataDir()).resolve(sheet.filePath);
                if (Files.exists(path)) {
                    try {
                        byte[] data = Files.readAllBytes(path);
                        String attachmentName = (sheet.storedName != null && !sheet.storedName.isBlank())
                                ? sheet.storedName
                                : sheet.originalName;
                        mail.addAttachment(attachmentName, data, sheet.contentType);
                    } catch (IOException e) {
                        LOG.warnf("[SafetySheet] Impossible de lire le fichier %s pour la pièce jointe : %s",
                                sheet.filePath, e.getMessage());
                    }
                }
            }
            reactiveMailer.send(mail)
                    .subscribe().with(
                        ignored -> LOG.infof("[SafetySheet] Notification envoyée (To=%d, Cc=%d, pièces jointes=%d) pour le créneau #%d",
                                    toRecipients.size(), ccRecipients.size(), sheets.size(), slot.id),
                            e -> LOG.errorf(e, "[SafetySheet] Erreur lors de l'envoi groupé pour le créneau #%d", slot.id)
                    );
        } catch (Exception e) {
            LOG.errorf(e, "[SafetySheet] Erreur lors de l'envoi groupé pour le créneau #%d", slot.id);
        }
    }

    public void sendNotificationAsync(DiveSlot slot, List<SlotSafetySheet> sheets,
                                      boolean isFollowUp, String additionalEmails) {
        // Charger d'abord la config dans le thread de requête, puis déléguer l'IO fichier au worker async.
        String recipientsCsv = configService.getSafetySheetNotificationEmails();
        String siteName = configService.getSiteName();

        CompletableFuture.runAsync(() -> sendNotification(slot, sheets, isFollowUp, additionalEmails, recipientsCsv, siteName))
                .exceptionally(e -> {
                    LOG.errorf(e, "[SafetySheet] Echec de l'envoi asynchrone pour le créneau #%d", slot.id);
                    return null;
                });
    }

    private String buildBody(String siteName, DiveSlot slot, String dateStr, int count, boolean isFollowUp) {
        String title = slot.title != null && !slot.title.isBlank() ? slot.title : "sans titre";
        String hours = slot.startTime + " – " + slot.endTime;
        String fichierMot = count > 1 ? count + " fiches de sécurité ont été" : "Une fiche de sécurité a été";
        String followUpNotice = isFollowUp
                ? "<p style=\"background:#fef3c7;border-left:4px solid #f59e0b;padding:8px 12px;border-radius:4px;\">"
                  + "ℹ️ Ce message fait suite à un précédent envoi concernant ce même créneau. "
                  + "De nouveaux fichiers ont été ajoutés.</p>"
                : "";
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
                  %s
                  <p>
                    %s déposée(s) sur le site <strong>%s</strong>
                    pour le créneau suivant et sont disponibles en pièce(s) jointe(s) :
                  </p>
                  <table style="border-collapse:collapse;width:100%%;margin:12px 0;">
                    <tr><td style="padding:6px 12px;background:#f0f9ff;font-weight:bold;">Date</td>
                        <td style="padding:6px 12px;">%s</td></tr>
                    <tr><td style="padding:6px 12px;background:#f0f9ff;font-weight:bold;">Horaires</td>
                        <td style="padding:6px 12px;">%s</td></tr>
                    <tr><td style="padding:6px 12px;background:#f0f9ff;font-weight:bold;">Intitulé</td>
                        <td style="padding:6px 12px;">%s</td></tr>
                  </table>
                  <p>Vous pouvez également vous connecter sur l'application pour consulter et télécharger les fichiers.</p>
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;" />
                  <p style="color:#6b7280;font-size:12px;">Système de réservation — %s</p>
                </body>
                </html>
                """.formatted(followUpNotice, fichierMot, siteName, dateStr, hours, title, siteName);
    }
}
