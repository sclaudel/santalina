package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.service.ConfigService;

import java.util.List;

@ApplicationScoped
public class BookingNotificationMailer {

    private static final Logger LOG = Logger.getLogger(BookingNotificationMailer.class);

    @Inject
    Mailer mailer;

    @Inject
    ConfigService configService;

    @Inject
    DivingConfig config;

    // -------------------------------------------------------------------------
    // Créneau simple
    // -------------------------------------------------------------------------

    public void sendSlotCreatedNotification(DiveSlot slot) {
        String notifEmail = configService.getNotificationBookingEmail();
        if (notifEmail == null || notifEmail.isBlank()) {
            return;
        }

        String slotLabel = slot.title != null && !slot.title.isBlank()
                ? slot.title
                : "Créneau du " + slot.slotDate;
        String typeInfo = (slot.slotType != null && !slot.slotType.isBlank())
                ? "<tr><td style='padding:4px 8px;color:#6b7280;'>Type :</td><td style='padding:4px 8px;'>" + slot.slotType + "</td></tr>"
                : "";
        String clubInfo = (slot.club != null && !slot.club.isBlank())
                ? "<tr><td style='padding:4px 8px;color:#6b7280;'>Club :</td><td style='padding:4px 8px;'>" + slot.club + "</td></tr>"
                : "";
        String notesInfo = (slot.notes != null && !slot.notes.isBlank())
                ? "<tr><td style='padding:4px 8px;color:#6b7280;'>Notes :</td><td style='padding:4px 8px;'>" + slot.notes + "</td></tr>"
                : "";
        String creator = slot.createdBy != null ? slot.createdBy.fullName() : "Inconnu";

        String body = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #1e40af;">🤿 Nouveau cr&eacute;neau cr&eacute;&eacute;</h2>
                  <p>Un nouveau cr&eacute;neau de plong&eacute;e vient d'&ecirc;tre cr&eacute;&eacute;e.</p>
                  <table style="border-collapse:collapse; width:100%%; margin:16px 0;">
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Titre :</td><td style='padding:4px 8px;'><strong>%s</strong></td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Date :</td><td style='padding:4px 8px;'>%s</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Horaire :</td><td style='padding:4px 8px;'>%s – %s</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Places :</td><td style='padding:4px 8px;'>%d</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Cr&eacute;&eacute; par :</td><td style='padding:4px 8px;'>%s</td></tr>
                    %s
                    %s
                    %s
                  </table>
                  <p style="color:#9ca3af;font-size:11px;margin-top:20px;">💡 Pour ne plus recevoir ces notifications,
                  <a href="%s" style="color:#9ca3af;">modifiez la configuration de notification</a>
                  dans les param&egrave;tres administrateurs.</p>
                  <hr style="border: 1px solid #e5e7eb; margin-top: 30px;" />
                  <p style="color: #6b7280; font-size: 12px;">Syst&egrave;me de r&eacute;servation — Santalina</p>
                </body>
                </html>
                """.formatted(
                slotLabel,
                slot.slotDate,
                slot.startTime, slot.endTime,
                slot.diverCount,
                creator,
                typeInfo,
                clubInfo,
                notesInfo,
                config.baseUrl() + "/?goto=admin"
        );

        String siteName = configService.getSiteName();
        String subject = "[" + siteName + "] Nouveau créneau — " + slotLabel;
        sendToAll(notifEmail, subject, body);
    }

    // -------------------------------------------------------------------------
    // Série récurrente — un seul mail récapitulatif
    // -------------------------------------------------------------------------

    public void sendRecurringSlotsSummary(List<DiveSlot> slots) {
        if (slots == null || slots.isEmpty()) return;
        String notifEmail = configService.getNotificationBookingEmail();
        if (notifEmail == null || notifEmail.isBlank()) return;

        DiveSlot first   = slots.get(0);
        String slotLabel = first.title != null && !first.title.isBlank()
                ? first.title
                : "Créneau " + first.startTime + "–" + first.endTime;
        String creator   = first.createdBy != null ? first.createdBy.fullName() : "Inconnu";

        String typeInfo = (first.slotType != null && !first.slotType.isBlank())
                ? "<tr><td style='padding:4px 8px;color:#6b7280;'>Type :</td><td style='padding:4px 8px;'>" + first.slotType + "</td></tr>"
                : "";
        String clubInfo = (first.club != null && !first.club.isBlank())
                ? "<tr><td style='padding:4px 8px;color:#6b7280;'>Club :</td><td style='padding:4px 8px;'>" + first.club + "</td></tr>"
                : "";
        String notesInfo = (first.notes != null && !first.notes.isBlank())
                ? "<tr><td style='padding:4px 8px;color:#6b7280;'>Notes :</td><td style='padding:4px 8px;'>" + first.notes + "</td></tr>"
                : "";

        StringBuilder dateListHtml = new StringBuilder();
        for (DiveSlot s : slots) {
            dateListHtml.append("<li style='margin-bottom:2px;'>").append(s.slotDate).append("</li>");
        }

        String body = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #1e40af;">🔁 %d cr&eacute;neau(x) r&eacute;current(s) cr&eacute;&eacute;(s)</h2>
                  <p>Une s&eacute;rie de <strong>%d cr&eacute;neaux</strong> de plong&eacute;e vient d'&ecirc;tre cr&eacute;&eacute;e.</p>
                  <table style="border-collapse:collapse; width:100%%; margin:16px 0;">
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Titre :</td><td style='padding:4px 8px;'><strong>%s</strong></td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Horaire :</td><td style='padding:4px 8px;'>%s – %s</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Places :</td><td style='padding:4px 8px;'>%d</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Cr&eacute;&eacute; par :</td><td style='padding:4px 8px;'>%s</td></tr>
                    %s
                    %s
                    %s
                  </table>
                  <h3 style="color:#374151; font-size:14px; margin-bottom:8px;">Dates cr&eacute;&eacute;es (%d)</h3>
                  <ul style="columns:3; -webkit-columns:3; margin:0; padding-left:20px; font-size:13px; color:#374151;">
                    %s
                  </ul>
                  <p style="color:#9ca3af;font-size:11px;margin-top:20px;">💡 Pour ne plus recevoir ces notifications,
                  <a href="%s" style="color:#9ca3af;">modifiez la configuration de notification</a>
                  dans les param&egrave;tres administrateurs.</p>
                  <hr style="border: 1px solid #e5e7eb; margin-top: 30px;" />
                  <p style="color: #6b7280; font-size: 12px;">Syst&egrave;me de r&eacute;servation — Santalina</p>
                </body>
                </html>
                """.formatted(
                slots.size(),
                slots.size(),
                slotLabel,
                first.startTime, first.endTime,
                first.diverCount,
                creator,
                typeInfo, clubInfo, notesInfo,
                slots.size(),
                dateListHtml.toString(),
                config.baseUrl() + "/?goto=admin"
        );

        String siteName = configService.getSiteName();
        String subject  = "[" + siteName + "] " + slots.size() + " créneaux récurrents — " + slotLabel;
        sendToAll(notifEmail, subject, body);
    }

    // -------------------------------------------------------------------------
    // Helper interne
    // -------------------------------------------------------------------------

    private void sendToAll(String notifEmail, String subject, String body) {
        String configUrl = config.baseUrl() + "/?goto=admin";
        String[] recipients = notifEmail.split(",");
        for (String recipient : recipients) {
            String email = recipient.trim();
            if (email.isBlank()) continue;
            LOG.infof("Envoi mail [%s] à %s", subject, email);
            try {
                mailer.send(Mail.withHtml(email, subject, body)
                        .addHeader("List-Unsubscribe", "<" + configUrl + ">")
                        .addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click"));
            } catch (Exception e) {
                LOG.errorf(e, "Échec de l'envoi du mail à %s", email);
            }
        }
    }
}
