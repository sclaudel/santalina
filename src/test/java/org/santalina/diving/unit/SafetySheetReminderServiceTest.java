package org.santalina.diving.unit;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.santalina.diving.mail.WaitingListMailer;
import org.santalina.diving.service.ConfigService;
import org.santalina.diving.service.SafetySheetReminderService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de SafetySheetReminderService.
 * La base H2 est vide : aucun créneau n'existe sauf ceux créés dans les tests.
 */
@QuarkusTest
class SafetySheetReminderServiceTest {

    @Inject
    SafetySheetReminderService reminderService;

    @InjectMock
    ConfigService configService;

    @InjectMock
    WaitingListMailer mailer;

    // ── Rappel désactivé ────────────────────────────────────────────────────

    @Test
    void sendPendingReminders_shouldDoNothing_whenDisabled() {
        when(configService.isNotifSafetyReminderEnabled()).thenReturn(false);

        reminderService.sendPendingReminders();

        verify(mailer, never()).sendSafetySheetReminder(any(), any(), any(), any());
    }

    // ── Rappel activé, aucun créneau en base ────────────────────────────────

    @Test
    void sendPendingReminders_shouldSendNoMail_whenNoPastSlots() {
        when(configService.isNotifSafetyReminderEnabled()).thenReturn(true);
        when(configService.getSafetyReminderDelayDays()).thenReturn(3);
        when(configService.getSafetyReminderActivationDate()).thenReturn(LocalDate.now().minusMonths(1));

        reminderService.sendPendingReminders();

        verify(mailer, never()).sendSafetySheetReminder(any(), any(), any(), any());
    }

    // ── Date d'activation dans le futur ─────────────────────────────────────

    @Test
    void sendPendingReminders_shouldSendNoMail_whenActivationDateIsNull() {
        // Sans date d'activation : la requête n'a pas de borne inférieure.
        // La base H2 de test étant vide, aucun mail ne doit partir.
        when(configService.isNotifSafetyReminderEnabled()).thenReturn(true);
        when(configService.getSafetyReminderDelayDays()).thenReturn(3);
        when(configService.getSafetyReminderActivationDate()).thenReturn(null);

        reminderService.sendPendingReminders();

        verify(mailer, never()).sendSafetySheetReminder(any(), any(), any(), any());
    }
}
