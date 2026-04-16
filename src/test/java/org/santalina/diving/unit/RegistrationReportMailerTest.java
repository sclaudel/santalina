package org.santalina.diving.unit;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.User;
import org.santalina.diving.mail.RegistrationReportMailer;
import org.santalina.diving.service.ConfigService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de RegistrationReportMailer.
 * Vérifie la génération CSV (ordre, colonnes, tri par club) et le corps du mail envoyé.
 */
@QuarkusTest
class RegistrationReportMailerTest {

    @Inject
    RegistrationReportMailer mailer;

    @Inject
    MockMailbox mailbox;

    @InjectMock
    ConfigService configService;

    @BeforeEach
    void setup() {
        mailbox.clear();
        when(configService.getSiteName()).thenReturn("SiteTest");
        when(configService.getReportEmailRecipients()).thenReturn("admin@test.com");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private User buildUser(String lastName, String firstName, String email, String club) {
        User u = new User();
        u.lastName     = lastName;
        u.firstName    = firstName;
        u.email        = email;
        u.club         = club;
        u.licenseNumber = null;
        u.createdAt    = LocalDateTime.of(2024, 3, 15, 10, 0);
        return u;
    }

    // ── buildCsv — colonnes ──────────────────────────────────────────────────

    @Test
    void buildCsv_shouldStartWithBom() {
        String csv = mailer.buildCsv(List.of());
        assertTrue(csv.startsWith("\uFEFF"), "Le CSV doit commencer par un BOM UTF-8");
    }

    @Test
    void buildCsv_shouldContainClubFirstColumn() {
        String csv = mailer.buildCsv(List.of());
        // En-tête : BOM + header sont sur la même ligne (ligne 0)
        String header = csv.lines().findFirst().orElse("").replace("\uFEFF", "");
        assertTrue(header.startsWith("Club;"), "La première colonne doit être 'Club'");
    }

    @Test
    void buildCsv_shouldContainExpectedHeaders() {
        String csv = mailer.buildCsv(List.of());
        String header = csv.lines().findFirst().orElse("").replace("\uFEFF", "");
        assertTrue(header.contains("Club"), "En-tête doit contenir 'Club'");
        assertTrue(header.contains("Nom"), "En-tête doit contenir 'Nom'");
        assertTrue(header.contains("Pr\u00e9nom"), "En-tête doit contenir 'Prénom'");
        assertTrue(header.contains("E-mail"), "En-tête doit contenir 'E-mail'");
        assertTrue(header.contains("Licence"), "En-tête doit contenir 'Licence'");
        assertTrue(header.contains("Date"), "En-tête doit contenir 'Date'");
    }

    @Test
    void buildCsv_shouldContainUserData() {
        User u = buildUser("DUPONT", "Jean", "jean@test.com", "Club Alpha");
        String csv = mailer.buildCsv(List.of(u));
        assertTrue(csv.contains("DUPONT"), "CSV doit contenir le nom");
        assertTrue(csv.contains("Jean"), "CSV doit contenir le prénom");
        assertTrue(csv.contains("jean@test.com"), "CSV doit contenir l'email");
        assertTrue(csv.contains("Club Alpha"), "CSV doit contenir le club");
    }

    @Test
    void buildCsv_shouldSortByClubThenLastName() {
        User u1 = buildUser("MARTIN", "Alice", "alice@test.com", "Club Zeta");
        User u2 = buildUser("DUPONT", "Bob", "bob@test.com", "Club Alpha");
        User u3 = buildUser("BERNARD", "Claire", "claire@test.com", "Club Alpha");

        String csv = mailer.buildCsv(List.of(u1, u2, u3));
        // Lines: BOM+header, then data
        String[] lines = csv.split("\n");
        // lines[0] = BOM + header line
        // lines[1] first data row should be from Club Alpha
        assertTrue(lines[1].startsWith("Club Alpha"), "Première ligne de données : Club Alpha (tri alphabétique)");
        // BERNARD before DUPONT within the same club
        int bernardIdx = -1, dupontIdx = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].contains("BERNARD")) bernardIdx = i;
            if (lines[i].contains("DUPONT")) dupontIdx = i;
        }
        assertTrue(bernardIdx < dupontIdx, "BERNARD doit apparaître avant DUPONT dans le même club");
        // Club Zeta last
        assertTrue(lines[lines.length - 1].startsWith("Club Zeta"), "Dernière ligne : Club Zeta");
    }

    @Test
    void buildCsv_nullClubShouldAppearLast() {
        User u1 = buildUser("DUPONT", "Jean", "jean@test.com", "Club Alpha");
        User u2 = buildUser("MARTIN", "Luc", "luc@test.com", null);

        String csv = mailer.buildCsv(List.of(u1, u2));
        String[] lines = csv.split("\n");
        // L'utilisateur sans club doit apparaître après Club Alpha
        int alphaIdx = -1, nullIdx = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].contains("DUPONT")) alphaIdx = i;
            if (lines[i].contains("MARTIN")) nullIdx = i;
        }
        assertTrue(alphaIdx < nullIdx, "Club Alpha doit apparaître avant les entrées sans club");
    }

    // ── Envoi du rapport ─────────────────────────────────────────────────────

    @Test
    void sendReport_shouldSendMailToConfiguredRecipient() {
        LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0);
        mailer.sendReport(since, List.of(buildUser("DUPONT", "Jean", "jean@test.com", "Club Alpha")));

        var mails = mailbox.getMailsSentTo("admin@test.com");
        assertEquals(1, mails.size(), "Un mail doit être envoyé au destinataire configuré");
    }

    @Test
    void sendReport_shouldAttachCsvFile() {
        LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0);
        mailer.sendReport(since, List.of(buildUser("DUPONT", "Jean", "jean@test.com", "Club Alpha")));

        var mails = mailbox.getMailsSentTo("admin@test.com");
        assertFalse(mails.get(0).getAttachments().isEmpty(), "Le mail doit contenir une pièce jointe CSV");
        String attachName = mails.get(0).getAttachments().get(0).getName();
        assertTrue(attachName.endsWith(".csv"), "La pièce jointe doit avoir l'extension .csv");
    }

    @Test
    void sendReport_htmlBodyShouldMentionCodep() {
        LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0);
        mailer.sendReport(since, List.of(buildUser("DUPONT", "Jean", "jean@test.com", "Club Alpha")));

        String html = mailbox.getMailsSentTo("admin@test.com").get(0).getHtml();
        assertTrue(html.contains("CODEP"), "Le corps du mail doit mentionner le CODEP");
    }

    @Test
    void sendReport_htmlBodyShouldMentionAdministrateur() {
        LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0);
        mailer.sendReport(since, List.of(buildUser("DUPONT", "Jean", "jean@test.com", "Club Alpha")));

        String html = mailbox.getMailsSentTo("admin@test.com").get(0).getHtml();
        assertTrue(html.contains("administrateur"), "Le corps du mail doit mentionner 'administrateur'");
    }

    @Test
    void sendReport_withExplicitRecipients_shouldSendToSpecifiedAddress() {
        LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0);
        mailer.sendReport(since, List.of(buildUser("DUPONT", "Jean", "jean@test.com", "Club Alpha")), "president@club.fr");

        assertTrue(mailbox.getMailsSentTo("president@club.fr").size() == 1,
                "Le mail doit être envoyé au destinataire explicite");
    }

    @Test
    void sendReport_withNoRecipients_shouldNotSendMail() {
        when(configService.getReportEmailRecipients()).thenReturn("");
        LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0);
        mailer.sendReport(since, List.of(buildUser("DUPONT", "Jean", "jean@test.com", "Club Alpha")));

        assertTrue(mailbox.getMailsSentTo("admin@test.com").isEmpty(),
                "Aucun mail ne doit être envoyé si aucun destinataire n'est configuré");
    }
}
