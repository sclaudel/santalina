package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.WaitingListEntry;
import org.santalina.diving.service.ConfigService;

@ApplicationScoped
public class WaitingListMailer {

    private static final Logger LOG = Logger.getLogger(WaitingListMailer.class);

    @Inject
    Mailer mailer;

    @Inject
    ConfigService configService;

    // =========================================================================
    // Mail au plongeur : inscription en liste d'attente
    // =========================================================================

    public void sendWaitingListConfirmation(WaitingListEntry entry, DiveSlot slot) {
        if (entry.email == null || entry.email.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Inscription en liste d'attente \u2014 " + slotLabel;

        String body = "<html><body style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
            + "<h2 style=\"color:#1e40af;\">\uD83E\uDD3F Inscription en liste d'attente</h2>"
            + "<p>Bonjour <strong>" + entry.firstName + " " + entry.lastName + "</strong>,</p>"
            + "<p>Votre demande d'inscription pour le cr\u00e9neau suivant a bien \u00e9t\u00e9 enregistr\u00e9e."
            + " Le directeur de plong\u00e9e examinera votre demande et vous informera de sa d\u00e9cision.</p>"
            + "<table style=\"border-collapse:collapse;width:100%;margin:16px 0;\">"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Cr\u00e9neau :</td><td style=\"padding:4px 8px;\"><strong>" + slotLabel + "</strong></td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Date :</td><td style=\"padding:4px 8px;\">" + slot.slotDate + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Horaire :</td><td style=\"padding:4px 8px;\">" + slot.startTime + " \u2013 " + slot.endTime + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Votre niveau :</td><td style=\"padding:4px 8px;\">" + entry.level + "</td></tr>"
            + "</table>"
            + "<p style=\"color:#6b7280;font-size:13px;\">Vous pouvez annuler votre inscription depuis la page du cr\u00e9neau tant que votre place n'a pas encore \u00e9t\u00e9 valid\u00e9e.</p>"
            + "<p style=\"color:#9ca3af;font-size:11px;margin-top:20px;\">\uD83D\uDCA1 Pour ne plus recevoir ce type de notification, connectez-vous et modifiez vos pr\u00e9f\u00e9rences dans votre <strong>Profil</strong> \u2192 <em>Notifications par e-mail</em>.</p>"
            + "<hr style=\"border:1px solid #e5e7eb;margin-top:30px;\"/>"
            + "<p style=\"color:#6b7280;font-size:12px;\">Syst\u00e8me de r\u00e9servation \u2014 " + siteName + "</p>"
            + "</body></html>";

        if (!shouldSend(configService.isNotifRegistrationEnabled(), "registration", entry.email, subject, body)) return;
        sendSingle(entry.email, subject, body);
    }

    // =========================================================================
    // Mail au plongeur : inscription valid\u00e9e (appel\u00e9 depuis DelayedNotificationService)
    // =========================================================================

    public void sendRegistrationApproved(String email, String firstName, String lastName, DiveSlot slot) {
        if (email == null || email.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Inscription valid\u00e9e \u2014 " + slotLabel;

        String body = "<html><body style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
            + "<h2 style=\"color:#16a34a;\">\u2705 Inscription valid\u00e9e !</h2>"
            + "<p>Bonjour <strong>" + firstName + " " + lastName + "</strong>,</p>"
            + "<p>Votre inscription pour le cr\u00e9neau suivant a \u00e9t\u00e9 <strong>valid\u00e9e</strong> par le directeur de plong\u00e9e."
            + " Vous faites maintenant partie des plongeurs confirm\u00e9s pour cette sortie.</p>"
            + "<table style=\"border-collapse:collapse;width:100%;margin:16px 0;\">"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Cr\u00e9neau :</td><td style=\"padding:4px 8px;\"><strong>" + slotLabel + "</strong></td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Date :</td><td style=\"padding:4px 8px;\">" + slot.slotDate + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Horaire :</td><td style=\"padding:4px 8px;\">" + slot.startTime + " \u2013 " + slot.endTime + "</td></tr>"
            + "</table>"
            + "<p>\u00c0 tr\u00e8s bient\u00f4t sous l'eau ! \uD83C\uDF0A</p>"
            + "<p style=\"color:#9ca3af;font-size:11px;margin-top:20px;\">\uD83D\uDCA1 Pour ne plus recevoir ce type de notification, connectez-vous et modifiez vos pr\u00e9f\u00e9rences dans votre <strong>Profil</strong> \u2192 <em>Notifications par e-mail</em>.</p>"
            + "<hr style=\"border:1px solid #e5e7eb;margin-top:30px;\"/>"
            + "<p style=\"color:#6b7280;font-size:12px;\">Syst\u00e8me de r\u00e9servation \u2014 " + siteName + "</p>"
            + "</body></html>";

        if (!shouldSend(configService.isNotifApprovedEnabled(), "approved", email, subject, body)) return;
        sendSingle(email, subject, body);
    }

    // =========================================================================
    // Mail au plongeur : remis en liste d'attente (appel\u00e9 depuis DelayedNotificationService)
    // =========================================================================

    public void sendMovedToWaitlist(String email, String firstName, String lastName,
                                    String level, DiveSlot slot) {
        if (email == null || email.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Remis en liste d'attente \u2014 " + slotLabel;

        String body = "<html><body style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
            + "<h2 style=\"color:#d97706;\">\u23F3 Remis en liste d'attente</h2>"
            + "<p>Bonjour <strong>" + firstName + " " + lastName + "</strong>,</p>"
            + "<p>Le directeur de plong\u00e9e vous a remis en <strong>liste d'attente</strong>"
            + " pour le cr\u00e9neau suivant. Votre demande reste enregistr\u00e9e et peut \u00eatre \u00e0 nouveau valid\u00e9e.</p>"
            + "<table style=\"border-collapse:collapse;width:100%;margin:16px 0;\">"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Cr\u00e9neau :</td><td style=\"padding:4px 8px;\"><strong>" + slotLabel + "</strong></td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Date :</td><td style=\"padding:4px 8px;\">" + slot.slotDate + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Horaire :</td><td style=\"padding:4px 8px;\">" + slot.startTime + " \u2013 " + slot.endTime + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Votre niveau :</td><td style=\"padding:4px 8px;\">" + level + "</td></tr>"
            + "</table>"
            + "<p style=\"color:#6b7280;font-size:13px;\">Vous pouvez annuler votre inscription depuis la page du cr\u00e9neau si vous le souhaitez.</p>"
            + "<p style=\"color:#9ca3af;font-size:11px;margin-top:20px;\">\uD83D\uDCA1 Pour ne plus recevoir ce type de notification, connectez-vous et modifiez vos pr\u00e9f\u00e9rences dans votre <strong>Profil</strong> \u2192 <em>Notifications par e-mail</em>.</p>"
            + "<hr style=\"border:1px solid #e5e7eb;margin-top:30px;\"/>"
            + "<p style=\"color:#6b7280;font-size:12px;\">Syst\u00e8me de r\u00e9servation \u2014 " + siteName + "</p>"
            + "</body></html>";

        if (!shouldSend(configService.isNotifMovedToWlEnabled(), "moved_to_waitlist", email, subject, body)) return;
        sendSingle(email, subject, body);
    }

    // =========================================================================
    // Mail au plongeur : inscription annul\u00e9e / supprim\u00e9e par le DP
    // =========================================================================

    public void sendCancellationToDiver(String email, String firstName, String lastName, DiveSlot slot) {
        if (email == null || email.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Inscription annul\u00e9e \u2014 " + slotLabel;

        String body = "<html><body style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
            + "<h2 style=\"color:#dc2626;\">\u274C Inscription annul\u00e9e</h2>"
            + "<p>Bonjour <strong>" + firstName + " " + lastName + "</strong>,</p>"
            + "<p>Le directeur de plong\u00e9e a annul\u00e9 votre inscription pour le cr\u00e9neau suivant :</p>"
            + "<table style=\"border-collapse:collapse;width:100%;margin:16px 0;\">"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Cr\u00e9neau :</td><td style=\"padding:4px 8px;\"><strong>" + slotLabel + "</strong></td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Date :</td><td style=\"padding:4px 8px;\">" + slot.slotDate + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Horaire :</td><td style=\"padding:4px 8px;\">" + slot.startTime + " \u2013 " + slot.endTime + "</td></tr>"
            + "</table>"
            + "<p style=\"color:#6b7280;font-size:13px;\">Pour toute question, rapprochez-vous du directeur de plong\u00e9e du cr\u00e9neau.</p>"
            + "<p style=\"color:#9ca3af;font-size:11px;margin-top:20px;\">\uD83D\uDCA1 Pour ne plus recevoir ce type de notification, connectez-vous et modifiez vos pr\u00e9f\u00e9rences dans votre <strong>Profil</strong> \u2192 <em>Notifications par e-mail</em>.</p>"
            + "<hr style=\"border:1px solid #e5e7eb;margin-top:30px;\"/>"
            + "<p style=\"color:#6b7280;font-size:12px;\">Syst\u00e8me de r\u00e9servation \u2014 " + siteName + "</p>"
            + "</body></html>";

        if (!shouldSend(configService.isNotifCancelledEnabled(), "cancelled", email, subject, body)) return;
        sendSingle(email, subject, body);
    }

    // =========================================================================
    // Mail au DP : un plongeur vient de s'inscrire en liste d'attente
    // =========================================================================

    public void sendNewRegistrationToDP(WaitingListEntry entry, DiveSlot slot, String dpEmail, boolean isAssignedDp) {
        if (dpEmail == null || dpEmail.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Nouvelle demande d'inscription \u2014 " + slotLabel;

        String certDate = entry.medicalCertDate != null ? entry.medicalCertDate.toString() : "non renseign\u00e9e";

        String body = "<html><body style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
            + "<h2 style=\"color:#1e40af;\">\uD83D\uDCCB Nouvelle demande d'inscription</h2>"
            + "<p>Un(e) plongeur(se) vient de s'inscrire en liste d'attente pour votre cr\u00e9neau :</p>"
            + "<table style=\"border-collapse:collapse;width:100%;margin:16px 0;\">"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Plongeur :</td><td style=\"padding:4px 8px;\"><strong>" + entry.firstName + " " + entry.lastName + "</strong></td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">E-mail :</td><td style=\"padding:4px 8px;\">" + entry.email + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Niveau :</td><td style=\"padding:4px 8px;\">" + entry.level + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Cr\u00e9neau :</td><td style=\"padding:4px 8px;\"><strong>" + slotLabel + "</strong></td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Date :</td><td style=\"padding:4px 8px;\">" + slot.slotDate + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Horaire :</td><td style=\"padding:4px 8px;\">" + slot.startTime + " \u2013 " + slot.endTime + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Cert. m\u00e9dical (d\u00e9but) :</td><td style=\"padding:4px 8px;\">" + certDate + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Licence FFESSM valid\u00e9e :</td><td style=\"padding:4px 8px;\">" + (entry.licenseConfirmed ? "\u2705 Oui" : "\u274C Non") + "</td></tr>"
            + (entry.comment != null && !entry.comment.isBlank()
                ? "<tr><td style=\"padding:4px 8px;color:#6b7280;vertical-align:top;\">Commentaire :</td><td style=\"padding:4px 8px;\">" + entry.comment + "</td></tr>"
                : "")
            + "</table>"
            + "<p style=\"color:#6b7280;font-size:13px;\">Connectez-vous pour valider ou refuser cette demande depuis la gestion du cr\u00e9neau.</p>"
            + "<p style=\"color:#9ca3af;font-size:11px;margin-top:20px;\">\uD83D\uDCA1 Pour ne plus recevoir ce type de notification, connectez-vous et modifiez vos pr\u00e9f\u00e9rences dans votre <strong>Profil</strong> \u2192 <em>Notifications par e-mail</em>.</p>"
            + "<hr style=\"border:1px solid #e5e7eb;margin-top:30px;\"/>"
            + "<p style=\"color:#6b7280;font-size:12px;\">Syst\u00e8me de r\u00e9servation \u2014 " + siteName + "</p>"
            + "</body></html>";

        if (!shouldSendDp(configService.isNotifDpNewRegEnabled(), "dp_new_registration", dpEmail, isAssignedDp, subject, body)) return;
        sendSingle(dpEmail, subject, body);
    }

    // =========================================================================    // Mail au DP/créateur : un plongeur a été ajouté directement sur le créneau
    // =========================================================================

    public void sendNewSlotDiverToDP(SlotDiver diver, DiveSlot slot, String dpEmail, boolean isAssignedDp) {
        if (dpEmail == null || dpEmail.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Nouvelle inscription — " + slotLabel;

        String body = "<html><body style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
            + "<h2 style=\"color:#1e40af;\">\uD83D\uDCCB Nouvelle inscription sur votre créneau</h2>"
            + "<p>Un(e) plongeur(se) a été inscrit(e) directement sur votre créneau :</p>"
            + "<table style=\"border-collapse:collapse;width:100%;margin:16px 0;\">"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Plongeur :</td><td style=\"padding:4px 8px;\"><strong>" + diver.firstName + " " + diver.lastName + "</strong></td></tr>"
            + (diver.email != null && !diver.email.isBlank() ? "<tr><td style=\"padding:4px 8px;color:#6b7280;\">E-mail :</td><td style=\"padding:4px 8px;\">" + diver.email + "</td></tr>" : "")
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Niveau :</td><td style=\"padding:4px 8px;\">" + (diver.level != null ? diver.level : "") + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Créneau :</td><td style=\"padding:4px 8px;\"><strong>" + slotLabel + "</strong></td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Date :</td><td style=\"padding:4px 8px;\">" + slot.slotDate + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Horaire :</td><td style=\"padding:4px 8px;\">" + slot.startTime + " – " + slot.endTime + "</td></tr>"
            + "</table>"
            + "<p style=\"color:#9ca3af;font-size:11px;margin-top:20px;\">\uD83D\uDCA1 Pour ne plus recevoir ce type de notification, connectez-vous et modifiez vos pr\u00e9f\u00e9rences dans votre <strong>Profil</strong> \u2192 <em>Notifications par e-mail</em>.</p>"
            + "<hr style=\"border:1px solid #e5e7eb;margin-top:30px;\"/>"
            + "<p style=\"color:#6b7280;font-size:12px;\">Système de réservation — " + siteName + "</p>"
            + "</body></html>";

        if (!shouldSendDp(configService.isNotifDpNewRegEnabled(), "dp_slot_registration", dpEmail, isAssignedDp, subject, body)) return;
        sendSingle(dpEmail, subject, body);
    }

    // =========================================================================    // Mail au DP : un plongeur a annul\u00e9 sa demande d'inscription
    // =========================================================================

    public void sendCancellationToDP(WaitingListEntry entry, DiveSlot slot, String dpEmail, boolean isAssignedDp) {
        if (dpEmail == null || dpEmail.isBlank()) return;

        String siteName  = configService.getSiteName();
        String slotLabel = slotLabel(slot);
        String subject   = "[" + siteName + "] Annulation d'inscription \u2014 " + slotLabel;

        String body = "<html><body style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
            + "<h2 style=\"color:#dc2626;\">\u274C Annulation d'inscription</h2>"
            + "<p>Le/la plongeur(se) <strong>" + entry.firstName + " " + entry.lastName + "</strong> a annul\u00e9 sa demande d'inscription pour le cr\u00e9neau suivant :</p>"
            + "<table style=\"border-collapse:collapse;width:100%;margin:16px 0;\">"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Cr\u00e9neau :</td><td style=\"padding:4px 8px;\"><strong>" + slotLabel + "</strong></td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Date :</td><td style=\"padding:4px 8px;\">" + slot.slotDate + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Horaire :</td><td style=\"padding:4px 8px;\">" + slot.startTime + " \u2013 " + slot.endTime + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">E-mail :</td><td style=\"padding:4px 8px;\">" + entry.email + "</td></tr>"
            + "<tr><td style=\"padding:4px 8px;color:#6b7280;\">Niveau :</td><td style=\"padding:4px 8px;\">" + entry.level + "</td></tr>"
            + "</table>"
            + "<p style=\"color:#6b7280;font-size:13px;\">Pensez \u00e0 v\u00e9rifier l'organisation de la sortie.</p>"
            + "<p style=\"color:#9ca3af;font-size:11px;margin-top:20px;\">\uD83D\uDCA1 Pour ne plus recevoir ce type de notification, connectez-vous et modifiez vos pr\u00e9f\u00e9rences dans votre <strong>Profil</strong> \u2192 <em>Notifications par e-mail</em>.</p>"
            + "<hr style=\"border:1px solid #e5e7eb;margin-top:30px;\"/>"
            + "<p style=\"color:#6b7280;font-size:12px;\">Syst\u00e8me de r\u00e9servation \u2014 " + siteName + "</p>"
            + "</body></html>";

        if (!shouldSendDp(configService.isNotifDpNewRegEnabled(), "dp_cancellation", dpEmail, isAssignedDp, subject, body)) return;
        sendSingle(dpEmail, subject, body);
    }

    // =========================================================================
    // Helpers priv\u00e9s
    // =========================================================================

    private boolean shouldSend(boolean globalEnabled, String notifType,
                                String recipientEmail, String subject, String body) {
        if (!globalEnabled) {
            LOG.infof("[NOTIF D\u00c9SACTIV\u00c9E][global][%s] Destinataire: %s | Sujet: %s | Contenu: %s",
                    notifType, recipientEmail, subject, body);
            return false;
        }
        User user = User.findByEmail(recipientEmail);
        if (user != null) {
            boolean userEnabled = switch (notifType) {
                case "registration"      -> user.notifOnRegistration;
                case "approved"          -> user.notifOnApproved;
                case "cancelled"         -> user.notifOnCancelled;
                case "moved_to_waitlist" -> user.notifOnMovedToWaitlist;
                default -> true;
            };
            if (!userEnabled) {
                LOG.infof("[NOTIF D\u00c9SACTIV\u00c9E][pr\u00e9f\u00e9rence][%s] Destinataire: %s | Sujet: %s | Contenu: %s",
                        notifType, recipientEmail, subject, body);
                return false;
            }
        }
        return true;
    }

    private boolean shouldSendDp(boolean globalEnabled, String notifType,
                                  String dpEmail, boolean isAssignedDp, String subject, String body) {
        if (!globalEnabled) {
            LOG.infof("[NOTIF D\u00c9SACTIV\u00c9E][global][%s] Destinataire: %s | Sujet: %s | Contenu: %s",
                    notifType, dpEmail, subject, body);
            return false;
        }
        User user = User.findByEmail(dpEmail);
        if (user != null) {
            boolean userEnabled = isAssignedDp ? user.notifOnDpRegistration : user.notifOnCreatorRegistration;
            if (!userEnabled) {
                LOG.infof("[NOTIF D\u00c9SACTIV\u00c9E][pr\u00e9f\u00e9rence][%s] Destinataire: %s | Sujet: %s | Contenu: %s",
                        notifType, dpEmail, subject, body);
                return false;
            }
        }
        return true;
    }

    private String slotLabel(DiveSlot slot) {
        return (slot.title != null && !slot.title.isBlank())
                ? slot.title
                : "Cr\u00e9neau du " + slot.slotDate;
    }

    private void sendSingle(String to, String subject, String body) {
        try {
            mailer.send(Mail.withHtml(to, subject, body));
        } catch (Exception e) {
            LOG.warnf("Impossible d'envoyer le mail '%s' \u00e0 %s : %s", subject, to, e.getMessage());
        }
    }
}
