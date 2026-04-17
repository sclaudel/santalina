package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mailer pour l'envoi du mail d'organisation de sortie par le directeur de plongée.
 * <p>
 * La liste des plongeurs est en CCI (BCC), le DP est en CC, le Reply-To pointe vers le DP.
 * </p>
 */
@ApplicationScoped
public class DpOrganizerMailer {

    private static final Logger LOG = Logger.getLogger(DpOrganizerMailer.class);

    /** Modèle par défaut variabilisé fourni à tout nouveau DP. */
    public static final String DEFAULT_TEMPLATE =
        "<p>Bonjour à tous,</p>" +
        "<p>Vous êtes inscrits à la sortie du <strong>{slotDate}</strong> à la {siteName} " +
        "(RDV à la {siteName} à {startTime}).</p>" +
        "<p>Voici quelques informations utiles à l'organisation de cette plongée.</p>" +
        "<h3>Horaires</h3>" +
        "<ul>" +
        "<li>Récupérez le matériel dans votre club, 1 bloc pour chaque plongée.</li>" +
        "<li>Pour ceux qui ont déjà tout le matériel, rendez-vous directement à la {siteName} à {startTime}. " +
        "Dans ce cas merci de me prévenir.</li>" +
        "<li>Prévoyez de quoi pique-niquer sur place après la plongée et de vous hydrater " +
        "(1 bouteille d'eau ou gourde par personne).</li>" +
        "</ul>" +
        "<h3>Administratif</h3>" +
        "<ul>" +
        "<li>Pensez à prendre les papiers nécessaires à la plongée :<br/>" +
        "Certificat médical, Carte de niveau, Carnet de plongée, Licence</li>" +
        "</ul>" +
        "<p><strong>==&gt; Pas de papiers = Pas de plongée</strong></p>" +
        "<ul>" +
        "<li>Prenez également vos fiches de progression et de suivi.</li>" +
        "<li>Pensez à contrôler vos papiers avant samedi matin ! Licence active et CACI valide !</li>" +
        "</ul>" +
        "<h3>Météo</h3>" +
        "<p>Pensez à prendre des vêtements chauds pour couvrir entre les 2 plongées.</p>" +
        "<p>La température de l'eau peut être inférieure à 10 °C au-delà de 3 m. " +
        "Prévoyez une combinaison adaptée, des gants, des chaussons et une cagoule. " +
        "Pensez également à prendre vos plombs !</p>" +
        "<h3>Règles de vie à la {siteName}</h3>" +
        "<ul>" +
        "<li>Les véhicules doivent être stationnés sur les emplacements prévus à cet effet. " +
        "Attention ! ne pas se garer sur la pelouse des locataires.</li>" +
        "<li>Au niveau du plan d'eau, aucun véhicule ne doit stationner. " +
        "On peut décharger/charger son matériel, puis se garer sur le parking au niveau du local.</li>" +
        "<li>Pour le pique-nique, nous le prendrons dans le local pour ne pas avoir froid.</li>" +
        "</ul>" +
        "<h3>Rappel sur le règlement spécifique de la {siteName}</h3>" +
        "<ul>" +
        "<li>Espace médian 6-20 m : chaque plongeur au-delà de 20 m doit être équipé de 2 premiers étages " +
        "et d'une lampe flash.</li>" +
        "<li>Espace lointain +20 m : le guide de palanquée doit avoir une source de lumière " +
        "(autre que la lampe flash).</li>" +
        "</ul>" +
        "<p>La présence d'un parachute de palier par palanquée (pour les encadrants) est obligatoire " +
        "(Code du sport).</p>" +
        "<p>En cas de contre-temps ou de retard, merci de me prévenir au plus tôt par mail ou téléphone " +
        "afin que je puisse ajuster mon organisation.</p>" +
        "<p>({dpPhone} ou {dpEmail}).</p>" +
        "<p>Je reste à votre disposition pour toutes questions complémentaires.</p>" +
        "<p>Bonne fin de semaine à tous et à bientôt,</p>" +
        "<p><strong>{dpName}</strong><br/>{dpPhone}</p>";

    @Inject
    Mailer mailer;

    /**
     * Envoie le mail d'organisation aux plongeurs du créneau.
     *
     * @param slot           Créneau concerné
     * @param dp             Directeur de plongée qui envoie le mail
     * @param divers         Liste des plongeurs inscrits
     * @param emailOverrides Emails saisis manuellement pour les plongeurs sans adresse (diverId → email)
     * @param subject        Objet du mail (peut contenir des variables)
     * @param htmlBody       Corps HTML du mail (peut contenir des variables)
     * @param siteName       Nom du site (pour la résolution des variables)
     * @param attachName     Nom du fichier joint (null = pas de pièce jointe)
     * @param attachBytes    Contenu binaire du fichier joint (null = pas de pièce jointe)
     * @param attachMime     Type MIME du fichier joint
     */
    public void sendOrganizationEmail(DiveSlot slot, User dp, List<SlotDiver> divers,
                                      Map<Long, String> emailOverrides,
                                      String subject, String htmlBody, String siteName,
                                      String attachName, byte[] attachBytes, String attachMime) {

        String resolvedSubject = resolveVariables(subject, slot, dp, siteName);
        String resolvedBody    = resolveVariables(htmlBody, slot, dp, siteName);
        String wrappedBody     = wrapHtml(resolvedBody, siteName);

        String dpEmail = dp != null && dp.email != null && !dp.email.isBlank() ? dp.email.trim() : null;

        boolean hasAttachment = attachName != null && !attachName.isBlank()
                && attachBytes != null && attachBytes.length > 0;

        // Le From est géré par quarkus.mailer.from (noreply@santalina.com) — SPF/DKIM/DMARC OK.
        // Le Reply-To pointe vers le DP pour que les réponses lui parviennent directement.

        // Collecter les adresses uniques des plongeurs
        List<String> recipients = divers.stream()
                .map(d -> {
                    if (d.email != null && !d.email.isBlank()) return d.email.trim();
                    if (emailOverrides != null) {
                        String ov = emailOverrides.get(d.id);
                        return (ov != null && !ov.isBlank()) ? ov.trim() : null;
                    }
                    return null;
                })
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .toList();

        if (recipients.isEmpty()) {
            LOG.warnf("Mail d'organisation (slotId=%d) : aucune adresse mail de plongeur disponible, envoi annulé.", slot.id);
            return;
        }

        // Envoyer un mail individuel par plongeur (visible séparément dans MailHog
        // et mieux accepté par les filtres anti-spam que le BCC massif).
        List<Mail> mails = new java.util.ArrayList<>();
        for (String to : recipients) {
            Mail m = Mail.withHtml(to, resolvedSubject, wrappedBody);
            if (dpEmail != null) {
                m.addHeader("Reply-To", dpEmail);
            }
            if (hasAttachment) {
                m.addAttachment(attachName, attachBytes, attachMime);
            }
            mails.add(m);
        }

        // Copie au DP avec la liste des destinataires en bas
        if (dpEmail != null) {
            StringBuilder recipientList = new StringBuilder(
                    "<hr style=\"border:1px solid #e5e7eb;margin-top:24px;\"/>\n"
                    + "<p style=\"color:#374151;font-size:13px;\"><strong>Ce mail a été envoyé aux adresses suivantes :</strong></p>\n"
                    + "<ul style=\"font-size:13px;color:#6b7280;\">\n");
            for (String r : recipients) {
                recipientList.append("  <li>").append(r).append("</li>\n");
            }
            recipientList.append("</ul>\n");
            String copyBody = wrapHtml(resolvedBody + recipientList, siteName);
            Mail copy = Mail.withHtml(dpEmail, "[Copie] " + resolvedSubject, copyBody);
            copy.addHeader("Reply-To", dpEmail);
            if (hasAttachment) {
                copy.addAttachment(attachName, attachBytes, attachMime);
            }
            mails.add(copy);
        }

        mailer.send(mails.toArray(new Mail[0]));
        LOG.infof("Mail d'organisation envoyé pour slotId=%d par %s — %d destinataire(s)%s.",
                slot.id, dpEmail, recipients.size(), hasAttachment ? " + PJ : " + attachName : "");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Construit l'adresse expéditeur au format RFC 2822 {@code "Prénom NOM" <email>}.
     * <p>
     * L'email passé doit être l'adresse serveur (ex. {@code noreply@santalina.com})
     * afin que SPF/DKIM/DMARC passent correctement. Le nom du DP est utilisé
     * comme display name pour que le destinataire identifie l'expéditeur.
     * </p>
     *
     * @return adresse formatée, ou {@code null} si l'email est absent
     */
    public static String buildFromAddress(User dp, String dpEmail) {
        if (dpEmail == null || dpEmail.isBlank()) return null;
        if (dp == null) return dpEmail;
        String name = dp.fullName().trim();
        if (name.isEmpty()) return dpEmail;
        // Échapper les guillemets éventuels dans le nom affiché (RFC 2822)
        String safeName = name.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + safeName + "\" <" + dpEmail + ">";
    }

    /**
     * Remplace les variables dans une chaîne de caractères.
     * Variables disponibles : {siteName}, {slotDate}, {startTime}, {endTime},
     *                         {slotTitle}, {dpName}, {dpEmail}, {dpPhone}
     */
    public static String resolveVariables(String template, DiveSlot slot, User dp, String siteName) {
        if (template == null) return "";

        // Date formatée "jeudi 12 avril 2026"
        String slotDate = slot.slotDate.format(
                DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));

        // Heures "09h15"
        String startTime = String.format("%02dh%02d",
                slot.startTime.getHour(), slot.startTime.getMinute());
        String endTime = String.format("%02dh%02d",
                slot.endTime.getHour(), slot.endTime.getMinute());

        String slotTitle = slot.title != null ? slot.title : "";
        String dpName    = dp != null ? dp.fullName() : "";
        String dpEmail   = dp != null && dp.email != null ? dp.email : "";
        String dpPhone   = dp != null && dp.phone != null ? dp.phone : "";

        return template
                .replace("{siteName}",  siteName != null ? siteName : "")
                .replace("{slotDate}",  slotDate)
                .replace("{startTime}", startTime)
                .replace("{endTime}",   endTime)
                .replace("{slotTitle}", slotTitle)
                .replace("{dpName}",    dpName)
                .replace("{dpEmail}",   dpEmail)
                .replace("{dpPhone}",   dpPhone);
    }

    private static String wrapHtml(String body, String siteName) {
        return "<!DOCTYPE html>\n<html lang=\"fr\">\n<head>\n"
                + "  <meta charset=\"UTF-8\" />\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n"
                + "</head>\n<body style=\"font-family:Arial,sans-serif;max-width:700px;margin:0 auto;\">\n"
                + body
                + "\n<hr style=\"border:1px solid #e5e7eb;margin-top:30px;\"/>\n"
                + "<p style=\"color:#6b7280;font-size:12px;\">Système de réservation — " + siteName + "</p>\n"
                + "</body></html>";
    }
}
