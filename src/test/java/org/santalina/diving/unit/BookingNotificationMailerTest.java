package org.santalina.diving.unit;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.mail.BookingNotificationMailer;
import org.santalina.diving.service.ConfigService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de BookingNotificationMailer.
 * Vérifie la structure HTML des mails et la présence du header List-Unsubscribe.
 */
@QuarkusTest
class BookingNotificationMailerTest {

    @Inject
    BookingNotificationMailer mailer;

    @Inject
    MockMailbox mailbox;

    @InjectMock
    ConfigService configService;

    private static final String ADMIN_EMAIL = "admin@test.com";
    // Valeur par défaut de app.diving.base-url (DivingConfig @WithDefault)
    private static final String BASE_URL    = "http://localhost:8085";

    @BeforeEach
    void setup() {
        mailbox.clear();
        when(configService.getNotificationBookingEmail()).thenReturn(ADMIN_EMAIL);
        when(configService.getSiteName()).thenReturn("SiteTest");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private DiveSlot buildSlot() {
        DiveSlot slot = new DiveSlot();
        slot.slotDate    = LocalDate.of(2099, 6, 15);
        slot.startTime   = LocalTime.of(9, 0);
        slot.endTime     = LocalTime.of(12, 0);
        slot.diverCount  = 8;
        slot.title       = "Sortie test";
        return slot;
    }

    // ── sendSlotCreatedNotification ──────────────────────────────────────────

    @Test
    void sendSlotCreatedNotification_shouldSendToAdminEmail() {
        mailer.sendSlotCreatedNotification(buildSlot());

        var mails = mailbox.getMailsSentTo(ADMIN_EMAIL);
        assertEquals(1, mails.size());
    }

    @Test
    void sendSlotCreatedNotification_htmlShouldContainDoctype() {
        mailer.sendSlotCreatedNotification(buildSlot());

        String html = mailbox.getMailsSentTo(ADMIN_EMAIL).get(0).getHtml();
        assertTrue(html.contains("<!DOCTYPE html>"), "Le mail doit contenir <!DOCTYPE html>");
        assertTrue(html.contains("<meta charset=\"UTF-8\""), "Le mail doit déclarer le charset UTF-8");
    }

    @Test
    void sendSlotCreatedNotification_shouldContainListUnsubscribeHeader() {
        mailer.sendSlotCreatedNotification(buildSlot());

        var mail = mailbox.getMailsSentTo(ADMIN_EMAIL).get(0);
        assertNotNull(mail.getHeaders().get("List-Unsubscribe"),
                "Le header List-Unsubscribe doit être présent");
        assertTrue(mail.getHeaders().get("List-Unsubscribe").stream()
                .anyMatch(v -> v.contains(BASE_URL + "/config")),
                "Le header List-Unsubscribe doit pointer vers la page config");
    }

    @Test
    void sendSlotCreatedNotification_bodyShouldContainUnsubscribeLink() {
        mailer.sendSlotCreatedNotification(buildSlot());

        String html = mailbox.getMailsSentTo(ADMIN_EMAIL).get(0).getHtml();
        assertTrue(html.contains(BASE_URL + "/config"),
                "Le corps du mail doit contenir le lien vers la page config");
    }

    @Test
    void sendSlotCreatedNotification_shouldNothingSendWhenEmailBlank() {
        when(configService.getNotificationBookingEmail()).thenReturn("  ");
        mailer.sendSlotCreatedNotification(buildSlot());

        assertTrue(mailbox.getMailsSentTo(ADMIN_EMAIL).isEmpty(),
                "Aucun mail ne doit être envoyé si la config email est vide");
    }

    // ── sendRecurringSlotsSummary ────────────────────────────────────────────

    @Test
    void sendRecurringSlotsSummary_shouldSendOneMailForSeveralSlots() {
        DiveSlot s1 = buildSlot();
        DiveSlot s2 = buildSlot();
        s2.slotDate = LocalDate.of(2099, 6, 22);

        mailer.sendRecurringSlotsSummary(List.of(s1, s2));

        var mails = mailbox.getMailsSentTo(ADMIN_EMAIL);
        assertEquals(1, mails.size(), "Un seul mail récapitulatif doit être envoyé");
    }

    @Test
    void sendRecurringSlotsSummary_htmlShouldContainDoctype() {
        mailer.sendRecurringSlotsSummary(List.of(buildSlot()));

        String html = mailbox.getMailsSentTo(ADMIN_EMAIL).get(0).getHtml();
        assertTrue(html.contains("<!DOCTYPE html>"), "Le mail doit contenir <!DOCTYPE html>");
    }

    @Test
    void sendRecurringSlotsSummary_shouldContainListUnsubscribeHeader() {
        mailer.sendRecurringSlotsSummary(List.of(buildSlot()));

        var mail = mailbox.getMailsSentTo(ADMIN_EMAIL).get(0);
        assertTrue(mail.getHeaders().get("List-Unsubscribe").stream()
                .anyMatch(v -> v.contains(BASE_URL + "/config")),
                "Le header List-Unsubscribe doit pointer vers la page config");
    }

    @Test
    void sendRecurringSlotsSummary_shouldDoNothingWhenListEmpty() {
        mailer.sendRecurringSlotsSummary(List.of());

        assertTrue(mailbox.getMailsSentTo(ADMIN_EMAIL).isEmpty(),
                "Aucun mail ne doit être envoyé pour une liste vide");
    }
}
