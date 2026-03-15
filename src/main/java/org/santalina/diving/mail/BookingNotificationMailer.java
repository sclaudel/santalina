package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.service.ConfigService;

@ApplicationScoped
public class BookingNotificationMailer {

    private static final Logger LOG = Logger.getLogger(BookingNotificationMailer.class);

    @Inject
    Mailer mailer;

    @Inject
    ConfigService configService;

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
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #1e40af;">🤿 Nouveau créneau créé</h2>
                  <p>Un nouveau créneau de plongée vient d'être créé.</p>
                  <table style="border-collapse:collapse; width:100%%; margin:16px 0;">
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Titre :</td><td style='padding:4px 8px;'><strong>%s</strong></td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Date :</td><td style='padding:4px 8px;'>%s</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Horaire :</td><td style='padding:4px 8px;'>%s – %s</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Places :</td><td style='padding:4px 8px;'>%d</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Créé par :</td><td style='padding:4px 8px;'>%s</td></tr>
                    %s
                    %s
                    %s
                  </table>
                  <hr style="border: 1px solid #e5e7eb; margin-top: 30px;" />
                  <p style="color: #6b7280; font-size: 12px;">Système de réservation — Santalina</p>
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
                notesInfo
        );

        LOG.infof("Envoi notification création créneau à %s (slot id=%d)", notifEmail, slot.id);
        try {
            mailer.send(Mail.withHtml(notifEmail, "🤿 Nouveau créneau — " + slotLabel, body));
        } catch (Exception e) {
            LOG.errorf(e, "Échec de l'envoi de la notification de création de créneau à %s", notifEmail);
        }
    }
}
