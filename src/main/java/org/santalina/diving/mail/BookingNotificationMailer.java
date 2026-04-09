package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.service.ConfigService;

import java.util.List;

@ApplicationScoped
public class BookingNotificationMailer {

    private static final Logger LOG = Logger.getLogger(BookingNotificationMailer.class);

    @Inject
    Mailer mailer;

    @Inject
    ConfigService configService;

    // -------------------------------------------------------------------------
    // Inscriptions plongeurs — file d'attente
    // -------------------------------------------------------------------------

    /** Mail envoyé au plongeur quand son inscription est mise en attente */
    public void sendRegistrationPendingToApplicant(DiveSlot slot, SlotDiver diver) {
        if (diver.email == null || diver.email.isBlank()) return;
        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Inscription en attente — " + slotLabel;
        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#1e40af;">🤿 Inscription enregistrée</h2>
                  <p>Bonjour <strong>%s</strong>,</p>
                  <p>Votre demande d'inscription sur le créneau <strong>%s</strong>
                     (le <strong>%s de %s à %s</strong>) a bien été reçue.</p>
                  <p>Elle est <strong>en attente de validation</strong> par le directeur de plongée.
                     Vous recevrez un mail dès qu'elle sera traitée.</p>
                  %s
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;"/>
                  <p style="color:#6b7280;font-size:12px;">%s — Système de réservation</p>
                </body>
                </html>
                """.formatted(
                diver.firstName,
                slotLabel,
                slot.slotDate, slot.startTime, slot.endTime,
                commentSection(diver.registrationComment),
                siteName);
        sendRegistrationMail(diver.email, subject, body);
    }

    /** Mail envoyé au plongeur quand son inscription est validée */
    public void sendRegistrationValidatedToApplicant(DiveSlot slot, SlotDiver diver) {
        if (diver.email == null || diver.email.isBlank()) return;
        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Inscription confirmée — " + slotLabel;
        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#059669;">✅ Inscription confirmée !</h2>
                  <p>Bonjour <strong>%s</strong>,</p>
                  <p>Votre inscription sur le créneau <strong>%s</strong>
                     (le <strong>%s de %s à %s</strong>) vient d'être <strong>validée</strong>
                     par le directeur de plongée.</p>
                  <p>Vous faites désormais partie des plongeurs confirmés sur cette sortie.
                     Préparez votre matériel !</p>
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;"/>
                  <p style="color:#6b7280;font-size:12px;">%s — Système de réservation</p>
                </body>
                </html>
                """.formatted(
                diver.firstName,
                slotLabel,
                slot.slotDate, slot.startTime, slot.endTime,
                siteName);
        sendRegistrationMail(diver.email, subject, body);
    }

    /** Mail envoyé au DP quand un plongeur annule sa participation */
    public void sendCancellationToDirector(DiveSlot slot, SlotDiver diver) {
        SlotDiver director = org.santalina.diving.domain.SlotDiver.findDirector(slot.id);
        if (director == null || director.email == null || director.email.isBlank()) return;
        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Annulation — " + diver.firstName + " " + diver.lastName + " — " + slotLabel;
        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#dc2626;">❌ Annulation d'inscription</h2>
                  <p>Bonjour,</p>
                  <p><strong>%s %s</strong> vient d'annuler sa participation au créneau
                     <strong>%s</strong> (le <strong>%s de %s à %s</strong>).</p>
                  <p>Une place s'est libérée. Pensez à consulter la liste d'attente.</p>
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;"/>
                  <p style="color:#6b7280;font-size:12px;">%s — Système de réservation</p>
                </body>
                </html>
                """.formatted(
                diver.firstName, diver.lastName,
                slotLabel,
                slot.slotDate, slot.startTime, slot.endTime,
                siteName);
        sendRegistrationMail(director.email, subject, body);
    }

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
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #1e40af;">🔁 %d créneau(x) récurrent(s) créé(s)</h2>
                  <p>Une série de <strong>%d créneaux</strong> de plongée vient d'être créée.</p>
                  <table style="border-collapse:collapse; width:100%%; margin:16px 0;">
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Titre :</td><td style='padding:4px 8px;'><strong>%s</strong></td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Horaire :</td><td style='padding:4px 8px;'>%s – %s</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Places :</td><td style='padding:4px 8px;'>%d</td></tr>
                    <tr><td style='padding:4px 8px;color:#6b7280;'>Créé par :</td><td style='padding:4px 8px;'>%s</td></tr>
                    %s
                    %s
                    %s
                  </table>
                  <h3 style="color:#374151; font-size:14px; margin-bottom:8px;">Dates créées (%d)</h3>
                  <ul style="columns:3; -webkit-columns:3; margin:0; padding-left:20px; font-size:13px; color:#374151;">
                    %s
                  </ul>
                  <hr style="border: 1px solid #e5e7eb; margin-top: 30px;" />
                  <p style="color: #6b7280; font-size: 12px;">Système de réservation — Santalina</p>
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
                dateListHtml.toString()
        );

        String siteName = configService.getSiteName();
        String subject  = "[" + siteName + "] " + slots.size() + " créneaux récurrents — " + slotLabel;
        sendToAll(notifEmail, subject, body);
    }

    // -------------------------------------------------------------------------
    // Helper interne
    // -------------------------------------------------------------------------

    /**
     * Envoie un mail lié aux inscriptions plongeurs.
     * Si le mode simulation est activé, redirige vers l'adresse configurée
     * en ajoutant une bannière indiquant le destinataire d'origine.
     */
    private void sendRegistrationMail(String recipient, String subject, String body) {
        String actualRecipient;
        String actualBody;
        if (configService.isRegistrationSimulationEnabled()) {
            String simAddress = configService.getRegistrationSimulationTo();
            if (simAddress == null || simAddress.isBlank()) {
                LOG.warnf("Mode simulation activé mais REGISTRATION_SIMULATION_TO est vide — mail ignoré");
                return;
            }
            actualRecipient = simAddress.trim();
            String banner = "<div style='background:#fef9c3;border:2px solid #eab308;padding:8px 12px;"
                    + "border-radius:6px;margin-bottom:16px;font-size:13px;'>"
                    + "<strong>[SIMULATION]</strong> Ce mail aurait été envoyé à : <code>" + recipient + "</code>"
                    + "</div>";
            actualBody = banner + body;
            subject = "[SIM] " + subject;
        } else {
            actualRecipient = recipient;
            actualBody      = body;
        }
        LOG.infof("Envoi mail inscription [%s] à %s", subject, actualRecipient);
        try {
            mailer.send(Mail.withHtml(actualRecipient, subject, actualBody));
        } catch (Exception e) {
            LOG.errorf(e, "Échec de l'envoi du mail à %s", actualRecipient);
        }
    }

    /** Retourne le label lisible d'un créneau */
    private static String slotLabel(DiveSlot slot) {
        return (slot.title != null && !slot.title.isBlank())
                ? slot.title
                : "Créneau du " + slot.slotDate;
    }

    /** Génère la section commentaire si non vide */
    private static String commentSection(String comment) {
        if (comment == null || comment.isBlank()) return "";
        return "<p><em>Votre commentaire pour le DP : " + comment + "</em></p>";
    }

    private void sendToAll(String notifEmail, String subject, String body) {
        String[] recipients = notifEmail.split(",");
        for (String recipient : recipients) {
            String email = recipient.trim();
            if (email.isBlank()) continue;
            LOG.infof("Envoi mail [%s] à %s", subject, email);
            try {
                mailer.send(Mail.withHtml(email, subject, body));
            } catch (Exception e) {
                LOG.errorf(e, "Échec de l'envoi du mail à %s", email);
            }
        }
    }
}
