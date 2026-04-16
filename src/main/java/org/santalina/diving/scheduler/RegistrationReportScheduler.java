package org.santalina.diving.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.santalina.diving.domain.User;
import org.santalina.diving.mail.RegistrationReportMailer;
import org.santalina.diving.service.ConfigService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Tâche planifiée : envoie périodiquement un rapport CSV des nouvelles inscriptions
 * par e-mail aux destinataires configurés dans l'administration.
 *
 * La fréquence de vérification est horaire ; l'envoi effectif se produit uniquement
 * lorsque le délai paramétré (en jours) est écoulé depuis le dernier envoi.
 */
@ApplicationScoped
public class RegistrationReportScheduler {

    private static final Logger LOG = Logger.getLogger(RegistrationReportScheduler.class);
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Inject
    ConfigService configService;

    @Inject
    RegistrationReportMailer reportMailer;

    @Scheduled(every = "1h", delayed = "5m")
    @Transactional
    public void checkAndSendReport() {
        if (!configService.isReportEmailEnabled()) {
            return;
        }

        int periodDays = configService.getReportEmailPeriodDays();
        if (periodDays <= 0) {
            return;
        }

        String recipients = configService.getReportEmailRecipients();
        if (recipients == null || recipients.isBlank()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = resolveLastSent(now, periodDays);

        // Pas encore le moment d'envoyer
        if (since == null) {
            return;
        }

        // Récupérer les utilisateurs activés inscrits depuis 'since'
        LocalDateTime sinceForQuery = since;
        List<User> newUsers = User.<User>find("activated = true AND createdAt >= ?1", sinceForQuery).list();

        if (newUsers.isEmpty()) {
            LOG.infof("Rapport d'inscriptions : aucun nouvel inscrit depuis le %s, envoi ignoré.", since.format(ISO_FMT));
        } else {
            reportMailer.sendReport(since, newUsers);
        }

        // Mise à jour de la date de dernier envoi dans tous les cas
        configService.updateReportEmailLastSent(now.format(ISO_FMT));
    }

    /**
     * Détermine la date de référence pour la requête utilisateurs.
     *
     * @return la date depuis laquelle chercher, ou {@code null} si l'envoi n'est pas encore dû.
     */
    private LocalDateTime resolveLastSent(LocalDateTime now, int periodDays) {
        String lastSentStr = configService.getReportEmailLastSent();

        if (lastSentStr == null || lastSentStr.isBlank()) {
            // Premier envoi : on considère la période précédente
            return now.minusDays(periodDays);
        }

        try {
            LocalDateTime lastSent = LocalDateTime.parse(lastSentStr, ISO_FMT);
            LocalDateTime nextDue  = lastSent.plusDays(periodDays);
            if (now.isBefore(nextDue)) {
                return null; // Pas encore l'heure
            }
            return lastSent;
        } catch (DateTimeParseException e) {
            LOG.warnf("Rapport d'inscriptions : impossible de lire la date du dernier envoi (%s), réinitialisation.", lastSentStr);
            return now.minusDays(periodDays);
        }
    }
}
