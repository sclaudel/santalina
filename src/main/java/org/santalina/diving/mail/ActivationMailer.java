package org.santalina.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.santalina.diving.config.DivingConfig;

@ApplicationScoped
public class ActivationMailer {

    private static final Logger LOG = Logger.getLogger(ActivationMailer.class);

    @Inject
    Mailer mailer;

    @Inject
    DivingConfig config;

    public void sendActivationEmail(String email, String name, String token) {
        String activationUrl = config.baseUrl() + "/activate?token=" + token;
        String body = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #1e40af;">🌊 Activez votre compte</h2>
                  <p>Bonjour <strong>%s</strong>,</p>
                  <p>Votre compte a bien été créé. Il vous reste une étape : cliquez sur le bouton
                     ci-dessous pour choisir votre mot de passe et activer votre accès.</p>
                  <p style="text-align: center; margin: 30px 0;">
                    <a href="%s"
                       style="background-color: #1e40af; color: white; padding: 12px 24px;
                              text-decoration: none; border-radius: 6px; font-size: 16px;">
                      Activer mon compte
                    </a>
                  </p>
                  <p>Ce lien est valable <strong>24 heures</strong>.</p>
                  <p>Si vous n'avez pas demandé la création de ce compte, ignorez ce message.</p>
                  <hr style="border: 1px solid #e5e7eb; margin-top: 30px;" />
                  <p style="color: #6b7280; font-size: 12px;">Système de réservation — Santalina</p>
                </body>
                </html>
                """.formatted(name, activationUrl);

        LOG.infof("Envoi email d'activation à %s", email);
        try {
            mailer.send(Mail.withHtml(email, "Activez votre compte Santalina", body));
            LOG.infof("Email d'activation envoyé avec succès à %s", email);
        } catch (Exception e) {
            LOG.errorf(e, "Échec de l'envoi de l'email d'activation à %s", email);
            throw e;
        }
    }
}
