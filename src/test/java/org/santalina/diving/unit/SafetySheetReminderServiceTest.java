package org.santalina.diving.unit;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.SlotSafetySheet;
import org.santalina.diving.mail.WaitingListMailer;
import org.santalina.diving.service.ConfigService;
import org.santalina.diving.service.SafetySheetReminderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    // ── Créneau éligible au rappel ──────────────────────────────────────────

    @Test
    @Transactional
    void sendPendingReminders_shouldSendMail_whenSlotIsEligible() {
        // Créer un créneau passé de 4 jours (délai = 3 jours)
        DiveSlot slot = new DiveSlot();
        slot.slotDate = LocalDate.now().minusDays(4);
        slot.startTime = LocalTime.of(9, 0);
        slot.endTime = LocalTime.of(12, 0);
        slot.diverCount = 10;
        slot.persist();

        // Ajouter un DP au créneau
        SlotDiver dp = new SlotDiver();
        dp.slot = slot;
        dp.firstName = "Jean";
        dp.lastName = "Dupont";
        dp.email = "dp@test.com";
        dp.level = "P3";
        dp.isDirector = true;
        dp.persist();

        when(configService.isNotifSafetyReminderEnabled()).thenReturn(true);
        when(configService.getSafetyReminderDelayDays()).thenReturn(3);
        when(configService.getSafetyReminderActivationDate()).thenReturn(null);

        reminderService.sendPendingReminders();

        verify(mailer).sendSafetySheetReminder(eq("dp@test.com"), eq("Jean"), eq("Dupont"), eq(slot));

        // Nettoyer
        dp.delete();
        slot.delete();
    }

    // ── Créneau avec fiche de sécurité ──────────────────────────────────────

    @Test
    @Transactional
    void sendPendingReminders_shouldNotSendMail_whenSafetySheetExists() {
        // Créer un créneau passé de 4 jours
        DiveSlot slot = new DiveSlot();
        slot.slotDate = LocalDate.now().minusDays(4);
        slot.startTime = LocalTime.of(9, 0);
        slot.endTime = LocalTime.of(12, 0);
        slot.diverCount = 10;
        slot.persist();

        // Créer une fiche de sécurité pour ce créneau
        SlotSafetySheet sheet = new SlotSafetySheet();
        sheet.slot = slot;
        sheet.originalName = "test.pdf";
        sheet.storedName = "uuid.pdf";
        sheet.filePath = "test/path";
        sheet.contentType = "application/pdf";
        sheet.fileSize = 1000;
        sheet.expiresAt = LocalDateTime.now().plusYears(1);
        sheet.persist();

        when(configService.isNotifSafetyReminderEnabled()).thenReturn(true);
        when(configService.getSafetyReminderDelayDays()).thenReturn(3);
        when(configService.getSafetyReminderActivationDate()).thenReturn(null);

        reminderService.sendPendingReminders();

        verify(mailer, never()).sendSafetySheetReminder(any(), any(), any(), any());

        // Nettoyer
        sheet.delete();
        slot.delete();
    }

    // ── Créneau trop récent ─────────────────────────────────────────────────

    @Test
    @Transactional
    void sendPendingReminders_shouldNotSendMail_whenSlotIsTooRecent() {
        // Créer un créneau passé de seulement 2 jours (délai = 3 jours)
        DiveSlot slot = new DiveSlot();
        slot.slotDate = LocalDate.now().minusDays(2);
        slot.startTime = LocalTime.of(9, 0);
        slot.endTime = LocalTime.of(12, 0);
        slot.diverCount = 10;
        slot.persist();

        when(configService.isNotifSafetyReminderEnabled()).thenReturn(true);
        when(configService.getSafetyReminderDelayDays()).thenReturn(3);
        when(configService.getSafetyReminderActivationDate()).thenReturn(null);

        reminderService.sendPendingReminders();

        verify(mailer, never()).sendSafetySheetReminder(any(), any(), any(), any());

        // Nettoyer
        slot.delete();
    }
}
