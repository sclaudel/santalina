package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.User;
import org.santalina.diving.service.ConfigService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Génère un rapport CSV des nouvelles inscriptions et l'envoie par e-mail
 * aux destinataires configurés dans l'administration.
 */
@ApplicationScoped
public class RegistrationReportMailer {

    private static final Logger LOG = Logger.getLogger(RegistrationReportMailer.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Inject
    Mailer mailer;

    @Inject
    ConfigService configService;

    /**
     * Envoie le rapport des inscriptions aux destinataires configurés.
     *
     * @param since date à partir de laquelle récupérer les inscriptions
     * @param users liste des utilisateurs inscrits depuis {@code since}
     */
    public void sendReport(LocalDateTime since, List<User> users) {
        String recipientsCsv = configService.getReportEmailRecipients();
        if (recipientsCsv == null || recipientsCsv.isBlank()) {
            LOG.warn("Rapport d'inscriptions : aucun destinataire configuré, envoi annulé.");
            return;
        }
        sendReport(since, users, recipientsCsv);
    }

    /**
     * Envoie le rapport des inscriptions aux destinataires explicitement fournis.
     *
     * @param since        date de référence (pour le sujet du mail)
     * @param users        liste des utilisateurs à inclure dans le rapport
     * @param recipientsCsv destinataires séparés par virgule ou point-virgule
     */
    public void sendReport(LocalDateTime since, List<User> users, String recipientsCsv) {
        List<String> recipients = Arrays.stream(recipientsCsv.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (recipients.isEmpty()) {
            LOG.warn("Rapport d'inscriptions : liste de destinataires vide après analyse, envoi annulé.");
            return;
        }

        String siteName = configService.getSiteName();
        byte[] csv = buildCsvBytes(users);

        String sinceFormatted = since.format(DATE_FMT);
        String subject = "[" + siteName + "] Rapport des nouvelles inscriptions depuis le " + sinceFormatted;

        int count = users.size();
        String body = buildEmailBody(siteName, sinceFormatted, count);

        String fileName = "inscriptions_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";

        for (String recipient : recipients) {
            try {
                mailer.send(
                        Mail.withHtml(recipient, subject, body)
                                .addAttachment(fileName, csv, "text/csv; charset=UTF-8")
                );
                LOG.infof("Rapport d'inscriptions envoyé à %s (%d inscrit(s) depuis le %s)", recipient, count, sinceFormatted);
            } catch (Exception e) {
                LOG.errorf(e, "Erreur lors de l'envoi du rapport d'inscriptions à %s", recipient);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Génération CSV
    // -------------------------------------------------------------------------

    /**
     * Génère le contenu CSV sous forme de tableau d'octets (UTF-8 avec BOM pour Excel).
     */
    public byte[] buildCsvBytes(List<User> users) {
        return buildCsv(users).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Génère le contenu CSV sous forme de chaîne de caractères (UTF-8 avec BOM pour Excel).
     */
    public String buildCsv(List<User> users) {
        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 pour compatibilité Excel
        sb.append('\uFEFF');
        sb.append("Club;Nom;Prénom;E-mail;Licence;Date d'inscription\n");
        users.stream()
             .sorted(Comparator.comparing(
                     (User u) -> u.club != null ? u.club : "\uFFFF",
                     String.CASE_INSENSITIVE_ORDER)
                     .thenComparing(u -> u.lastName != null ? u.lastName : "")
                     .thenComparing(u -> u.firstName != null ? u.firstName : ""))
             .forEach(user -> sb
                     .append(csvEscape(user.club)).append(';')
                     .append(csvEscape(user.lastName)).append(';')
                     .append(csvEscape(user.firstName)).append(';')
                     .append(csvEscape(user.email)).append(';')
                     .append(csvEscape(user.licenseNumber)).append(';')
                     .append(user.createdAt != null ? user.createdAt.format(DATE_FMT) : "")
                     .append('\n'));
        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // Corps du mail HTML
    // -------------------------------------------------------------------------

    private String buildEmailBody(String siteName, String sinceFormatted, int count) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#1e40af;">Rapport des nouvelles inscriptions</h2>
                  <p>Bonjour,</p>
                  <p>
                    Vous recevez ce message en tant qu'<strong>administrateur du site</strong>,
                    <strong>président de club</strong> ou <strong>responsable technique</strong>.
                  </p>
                  <p>
                    Vous trouverez en pièce jointe la liste des <strong>%d nouvelle(s) inscription(s)</strong>
                    enregistrée(s) sur <strong>%s</strong> depuis le <strong>%s</strong>.
                    Les inscriptions sont triées par club d'appartenance.
                  </p>
                  <div style="background:#fef9c3;border:1px solid #fbbf24;border-radius:6px;padding:12px 16px;margin:16px 0;">
                    <strong>⚠️ Action requise si nécessaire</strong><br />
                    Si un plongeur listé ne fait pas partie de votre club ou si son inscription vous semble
                    incorrecte, merci de le signaler auprès du <strong>comité départemental (CODEP)</strong>
                    compétent afin de régulariser la situation.
                  </div>
                  <p>Ce rapport est généré automatiquement. Pour modifier sa fréquence ou ses destinataires,
                  rendez-vous dans les paramètres administrateurs, rubrique <em>Rapport périodique</em>.</p>
                  <hr style="border:1px solid #e5e7eb;margin-top:30px;" />
                  <p style="color:#6b7280;font-size:12px;">Système de réservation — %s</p>
                </body>
                </html>
                """.formatted(count, siteName, sinceFormatted, siteName);
    }
}
