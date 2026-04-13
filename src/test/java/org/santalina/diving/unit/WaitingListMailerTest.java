package org.santalina.diving.unit;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.WaitingListEntry;
import org.santalina.diving.mail.WaitingListMailer;
import org.santalina.diving.service.ConfigService;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de WaitingListMailer.
 * Vérifie la structure HTML, le header List-Unsubscribe et le lien de préférences.
 */
@QuarkusTest
class WaitingListMailerTest {

    @Inject
    WaitingListMailer mailer;

    @Inject
    MockMailbox mailbox;

    @InjectMock
    ConfigService configService;

    private static final String DIVER_EMAIL = "plongeur@test.com";
    private static final String DP_EMAIL    = "dp@test.com";
    // Valeur par défaut de app.diving.base-url (DivingConfig @WithDefault)
    private static final String BASE_URL    = "http://localhost:8085";

    @BeforeEach
    void setup() {
        mailbox.clear();
        when(configService.getSiteName()).thenReturn("SiteTest");
        when(configService.isNotifRegistrationEnabled()).thenReturn(true);
        when(configService.isNotifApprovedEnabled()).thenReturn(true);
        when(configService.isNotifMovedToWlEnabled()).thenReturn(true);
        when(configService.isNotifCancelledEnabled()).thenReturn(true);
        when(configService.isNotifDpNewRegEnabled()).thenReturn(true);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private DiveSlot buildSlot() {
        DiveSlot slot = new DiveSlot();
        slot.slotDate   = LocalDate.of(2099, 6, 15);
        slot.startTime  = LocalTime.of(9, 0);
        slot.endTime    = LocalTime.of(12, 0);
        slot.diverCount = 8;
        slot.title      = "Sortie test";
        return slot;
    }

    private WaitingListEntry buildEntry() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.firstName = "Alice";
        entry.lastName  = "Martin";
        entry.email     = DIVER_EMAIL;
        entry.level     = "N2";
        return entry;
    }

    // ── sendWaitingListConfirmation ──────────────────────────────────────────

    @Test
    void sendWaitingListConfirmation_htmlShouldContainDoctype() {
        mailer.sendWaitingListConfirmation(buildEntry(), buildSlot());

        String html = mailbox.getMailsSentTo(DIVER_EMAIL).get(0).getHtml();
        assertTrue(html.contains("<!DOCTYPE html>"), "Le mail doit contenir <!DOCTYPE html>");
        assertTrue(html.contains("<meta charset=\"UTF-8\""), "Le mail doit déclarer le charset UTF-8");
    }

    @Test
    void sendWaitingListConfirmation_shouldContainListUnsubscribeHeader() {
        mailer.sendWaitingListConfirmation(buildEntry(), buildSlot());

        var mail = mailbox.getMailsSentTo(DIVER_EMAIL).get(0);
        assertTrue(mail.getHeaders().get("List-Unsubscribe").stream()
                .anyMatch(v -> v.contains(BASE_URL + "/?goto=profile")),
                "Le header List-Unsubscribe doit pointer vers /profile");
    }

    @Test
    void sendWaitingListConfirmation_bodyShouldContainProfileLink() {
        mailer.sendWaitingListConfirmation(buildEntry(), buildSlot());

        String html = mailbox.getMailsSentTo(DIVER_EMAIL).get(0).getHtml();
        assertTrue(html.contains(BASE_URL + "/?goto=profile"),
                "Le lien vers les préférences de notification doit figurer dans le corps");
    }

    @Test
    void sendWaitingListConfirmation_shouldNotSendWhenGlobalDisabled() {
        when(configService.isNotifRegistrationEnabled()).thenReturn(false);
        mailer.sendWaitingListConfirmation(buildEntry(), buildSlot());

        assertTrue(mailbox.getMailsSentTo(DIVER_EMAIL).isEmpty(),
                "Aucun mail ne doit être envoyé si la notification globale est désactivée");
    }

    // ── sendRegistrationApproved ─────────────────────────────────────────────

    @Test
    void sendRegistrationApproved_htmlShouldContainDoctype() {
        mailer.sendRegistrationApproved(DIVER_EMAIL, "Alice", "Martin", buildSlot());

        String html = mailbox.getMailsSentTo(DIVER_EMAIL).get(0).getHtml();
        assertTrue(html.contains("<!DOCTYPE html>"), "Le mail doit contenir <!DOCTYPE html>");
    }

    @Test
    void sendRegistrationApproved_shouldContainListUnsubscribeHeader() {
        mailer.sendRegistrationApproved(DIVER_EMAIL, "Alice", "Martin", buildSlot());

        var mail = mailbox.getMailsSentTo(DIVER_EMAIL).get(0);
        assertTrue(mail.getHeaders().get("List-Unsubscribe").stream()
                .anyMatch(v -> v.contains(BASE_URL + "/?goto=profile")),
                "Le header List-Unsubscribe doit pointer vers /profile");
    }

    // ── sendCancellationToDiver ──────────────────────────────────────────────

    @Test
    void sendCancellationToDiver_htmlShouldContainDoctype() {
        mailer.sendCancellationToDiver(DIVER_EMAIL, "Alice", "Martin", buildSlot());

        String html = mailbox.getMailsSentTo(DIVER_EMAIL).get(0).getHtml();
        assertTrue(html.contains("<!DOCTYPE html>"), "Le mail doit contenir <!DOCTYPE html>");
    }

    @Test
    void sendCancellationToDiver_bodyShouldContainProfileLink() {
        mailer.sendCancellationToDiver(DIVER_EMAIL, "Alice", "Martin", buildSlot());

        String html = mailbox.getMailsSentTo(DIVER_EMAIL).get(0).getHtml();
        assertTrue(html.contains(BASE_URL + "/?goto=profile"),
                "Le lien vers /profile doit figurer dans le corps du mail d'annulation");
    }

    // ── sendNewRegistrationToDP ──────────────────────────────────────────────

    @Test
    void sendNewRegistrationToDP_htmlShouldContainDoctype() {
        mailer.sendNewRegistrationToDP(buildEntry(), buildSlot(), DP_EMAIL, true);

        String html = mailbox.getMailsSentTo(DP_EMAIL).get(0).getHtml();
        assertTrue(html.contains("<!DOCTYPE html>"), "Le mail au DP doit contenir <!DOCTYPE html>");
    }

    @Test
    void sendNewRegistrationToDP_shouldContainListUnsubscribeHeader() {
        mailer.sendNewRegistrationToDP(buildEntry(), buildSlot(), DP_EMAIL, true);

        var mail = mailbox.getMailsSentTo(DP_EMAIL).get(0);
        assertTrue(mail.getHeaders().get("List-Unsubscribe").stream()
                .anyMatch(v -> v.contains(BASE_URL + "/?goto=profile")),
                "Le header List-Unsubscribe du mail DP doit pointer vers /profile");
    }
}
