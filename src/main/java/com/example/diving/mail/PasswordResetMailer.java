package com.example.diving.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PasswordResetMailer {

    @Inject
    Mailer mailer;

    public void sendResetEmail(String email, String name, String token) {
        String resetUrl = "http://localhost:8085/reset-password?token=" + token;
        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #1e40af;">🌊 Lac Plongée - Réinitialisation du mot de passe</h2>
                  <p>Bonjour <strong>%s</strong>,</p>
                  <p>Vous avez demandé la réinitialisation de votre mot de passe.</p>
                  <p>Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :</p>
                  <p style="text-align: center; margin: 30px 0;">
                    <a href="%s"
                       style="background-color: #1e40af; color: white; padding: 12px 24px;
                              text-decoration: none; border-radius: 6px; font-size: 16px;">
                      Réinitialiser mon mot de passe
                    </a>
                  </p>
                  <p>Ce lien est valable 30 minutes.</p>
                  <p>Si vous n'avez pas demandé cette réinitialisation, ignorez ce message.</p>
                  <hr style="border: 1px solid #e5e7eb; margin-top: 30px;" />
                  <p style="color: #6b7280; font-size: 12px;">Lac Plongée - Système de réservation</p>
                </body>
                </html>
                """.formatted(name, resetUrl);

        mailer.send(
                Mail.withHtml(email, "Réinitialisation de votre mot de passe - Lac Plongée", body)
        );
    }
}

