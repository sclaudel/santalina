package org.santalina.diving.unit;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotSafetySheet;
import org.santalina.diving.mail.SafetySheetMailer;
import org.santalina.diving.service.ConfigService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class SafetySheetMailerTest {

    @Inject
    SafetySheetMailer mailer;

    @Inject
    MockMailbox mailbox;

    @InjectMock
    ConfigService configService;

    @BeforeEach
    void setup() {
        mailbox.clear();
        when(configService.getSiteName()).thenReturn("SiteTest");
        when(configService.getSafetySheetNotificationEmails()).thenReturn("admin1@club.fr;admin2@club.fr");
    }

    private DiveSlot buildSlot() {
        DiveSlot slot = new DiveSlot();
        slot.id = 90L;
        slot.slotDate = LocalDate.of(2099, 6, 15);
        slot.startTime = LocalTime.of(9, 0);
        slot.endTime = LocalTime.of(12, 0);
        slot.title = "Sortie sécurité";
        return slot;
    }

    private void waitForMail(String recipient, int expectedCount) {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (mailbox.getMailsSentTo(recipient).size() >= expectedCount) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Attente interrompue");
            }
        }
        fail("Mail non reçu dans le délai pour " + recipient);
    }

    @Test
    void sendNotification_shouldUseConfiguredAsTo_andAdditionalAsCc() {
        DiveSlot slot = buildSlot();

        mailer.sendNotification(
                slot,
                List.<SlotSafetySheet>of(),
                false,
                "president@club.fr, responsable@club.fr, admin1@club.fr"
        );

        waitForMail("admin1@club.fr", 1);
        var mail = mailbox.getMailsSentTo("admin1@club.fr").get(0);

        assertEquals(List.of("admin1@club.fr", "admin2@club.fr"), mail.getTo());
        assertTrue(mail.getCc().contains("president@club.fr"));
        assertTrue(mail.getCc().contains("responsable@club.fr"));
        assertFalse(mail.getCc().contains("admin1@club.fr"));
    }

    @Test
    void sendNotification_whenConfiguredEmpty_shouldFallbackAdditionalInTo() {
        when(configService.getSafetySheetNotificationEmails()).thenReturn(" ");

        DiveSlot slot = buildSlot();
        mailer.sendNotification(slot, List.<SlotSafetySheet>of(), false, "president@club.fr, responsable@club.fr");

        waitForMail("president@club.fr", 1);
        var mail = mailbox.getMailsSentTo("president@club.fr").get(0);

        assertEquals(List.of("president@club.fr", "responsable@club.fr"), mail.getTo());
        assertTrue(mail.getCc() == null || mail.getCc().isEmpty());
    }

    @Test
    void sendNotificationAsync_followUp_shouldContainComplementInSubject() {
        DiveSlot slot = buildSlot();

        mailer.sendNotificationAsync(slot, List.<SlotSafetySheet>of(), true, null);

        waitForMail("admin1@club.fr", 1);
        var mail = mailbox.getMailsSentTo("admin1@club.fr").get(0);

        assertTrue(mail.getSubject().contains("(complément)"));
    }
}
