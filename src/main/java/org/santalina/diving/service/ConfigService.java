package org.santalina.diving.service;

import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.AppConfigEntry;
import org.santalina.diving.dto.ConfigDto.ConfigResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConfigService {

    private static final String KEY_MAX_DIVERS  = "max.divers";
    private static final String KEY_MIN_HOURS   = "slot.min.hours";
    private static final String KEY_MAX_HOURS   = "slot.max.hours";
    private static final String KEY_RESOLUTION  = "slot.resolution.minutes";
    private static final String KEY_SITE_NAME   = "site.name";
    private static final String KEY_SLOT_TYPES       = "slot.types";
    private static final String KEY_CLUBS             = "slot.clubs";
    private static final String KEY_LEVELS            = "diver.levels";
    private static final String KEY_DIVER_LEVELS       = "diver.levels.registration";
    private static final String KEY_DP_LEVELS          = "diver.levels.dp";
    private static final String KEY_PREPARED_LEVELS    = "diver.levels.prepared";
    private static final String KEY_APTITUDES          = "diver.aptitudes";
    private static final String KEY_PUBLIC_ACCESS     = "public.access";
    private static final String KEY_SELF_REGISTRATION = "self.registration";
    private static final String KEY_BOOKING_OPEN_HOUR    = "booking.open.hour";
    private static final String KEY_BOOKING_CLOSE_HOUR   = "booking.close.hour";
    private static final String KEY_EXCLUSIVE_SLOT_TYPES = "slot.exclusive.types";
    private static final String KEY_DEFAULT_SLOT_HOURS   = "slot.default.hours";
    private static final String KEY_NOTIFICATION_BOOKING_EMAIL = "notification.booking.email";
    private static final String KEY_MAX_RECURRING_MONTHS       = "max.recurring.months";

    // -- Activation globale des notifications par type --
    private static final String KEY_NOTIF_REGISTRATION    = "notif.registration.enabled";
    private static final String KEY_NOTIF_APPROVED        = "notif.approved.enabled";
    private static final String KEY_NOTIF_CANCELLED       = "notif.cancelled.enabled";
    private static final String KEY_NOTIF_MOVED_TO_WL     = "notif.moved_to_waitlist.enabled";
    private static final String KEY_NOTIF_DP_NEW_REG      = "notif.dp.new_registration.enabled";

    private static final String KEY_MAINTENANCE_MODE           = "maintenance.mode";

    // -- Rappel fiche de sécurité --
    private static final String KEY_NOTIF_SAFETY_REMINDER           = "notif.safety_reminder.enabled";
    private static final String KEY_SAFETY_REMINDER_DELAY_DAYS      = "notif.safety_reminder.delay_days";
    private static final String KEY_SAFETY_REMINDER_EMAIL_BODY      = "notif.safety_reminder.email_body";
    private static final String KEY_SAFETY_REMINDER_ACTIVATION_DATE = "notif.safety_reminder.activation_date";

    // -- Rapport périodique d'inscriptions --
    private static final String KEY_REPORT_EMAIL_ENABLED     = "report.email.enabled";
    private static final String KEY_REPORT_EMAIL_PERIOD_DAYS = "report.email.period.days";
    private static final String KEY_REPORT_EMAIL_RECIPIENTS  = "report.email.recipients";
    private static final String KEY_REPORT_EMAIL_LAST_SENT   = "report.email.last.sent";

    private static final String KEY_DEFAULT_ORGANIZER_MAIL_TEMPLATE = "dp.organizer.mail.template.default";

    // -- Fiches de sécurité --
    private static final String KEY_SAFETY_SHEET_NOTIFICATION_EMAILS = "safety.sheet.notification.emails";
    private static final String KEY_SAFETY_SHEET_VIEWER_EMAILS       = "safety.sheet.viewer.emails";

    private static final String DEFAULT_SAFETY_REMINDER_BODY =
        "Ce rappel vous est envoyé car vous êtes le directeur de plongée du créneau du {slotDate} sur le site {siteName}.\n\n" +
        "Pensez à transmettre la fiche de sécurité remplie à votre club si ce n'est pas encore fait.";

    private static final String DEFAULT_SLOT_TYPES =
        "Club - Plongée|Club - Apnée|Club - Nage avec Palme|CODEP - Plongée|CODEP - Apnée|CODEP - Nage avec Palme|Externe - SDIS - Gendarmerie";
    private static final String DEFAULT_CLUBS  = "";
    private static final String DEFAULT_LEVELS =
        "Inconnu|E1|E2|Niveau 1|Niveau 2|Niveau 3|Niveau 4|Guide de Palanquée" +
        "|MF1|MF2|Moniteur|Directeur de plongée" +
        "|PADI Open Water|PADI Advanced|PADI Rescue" +
        "|Prepa-N1|Prepa-N2|Prepa-N3|Prepa-N4|Prepa-MF1|Prepa-MF2";
    private static final String DEFAULT_DIVER_LEVELS =
        "N1|N2|N3|N4|N5|E2|E3|E4|PE12|PE40|PE60|MF1|MF2";
    private static final String DEFAULT_DP_LEVELS =
        "N5|E3|E4|MF1|MF2";
    private static final String DEFAULT_PREPARED_LEVELS =
        "Aucun|N1|N2|N3|N4|N5|MF1|MF2|E1|E2|E3|E4|PE12|PE40|PE60" +
        "|PA20|PA40|PA60|PN|PNC|PB1|PB2|PV1|PV2";
    private static final String DEFAULT_APTITUDES =
        "PE12|PE20|PE40|PE60|PA12|PA20|PA40|PA60|E1|E2|E3|E4|GP";

    @Inject
    DivingConfig divingConfig;

    // ---- Getters publics ----

    public int getMaxDivers() {
        return getIntValue(KEY_MAX_DIVERS, divingConfig.maxDivers() > 0 ? divingConfig.maxDivers() : 25);
    }
    public int getSlotMinHours() {
        return getIntValue(KEY_MIN_HOURS, divingConfig.slotMinHours() > 0 ? divingConfig.slotMinHours() : 1);
    }
    public int getSlotMaxHours() {
        return getIntValue(KEY_MAX_HOURS, divingConfig.slotMaxHours() > 0 ? divingConfig.slotMaxHours() : 10);
    }
    public int getSlotResolutionMinutes() {
        return getIntValue(KEY_RESOLUTION, divingConfig.slotResolutionMinutes() > 0 ? divingConfig.slotResolutionMinutes() : 15);
    }
    public String getSiteName() {
        return getStringValue(KEY_SITE_NAME,
                divingConfig.siteName() != null && !divingConfig.siteName().isBlank()
                        ? divingConfig.siteName() : "Carrière de Saint-Lin");
    }
    public List<String> getSlotTypes() {
        return parseList(getStringValue(KEY_SLOT_TYPES, DEFAULT_SLOT_TYPES));
    }
    public List<String> getClubs() {
        return parseList(getStringValue(KEY_CLUBS, DEFAULT_CLUBS));
    }
    public List<String> getLevels() {
        return parseList(getStringValue(KEY_LEVELS, DEFAULT_LEVELS));
    }
    public List<String> getDiverLevels() {
        return parseList(getStringValue(KEY_DIVER_LEVELS, DEFAULT_DIVER_LEVELS));
    }
    public List<String> getDpLevels() {
        return parseList(getStringValue(KEY_DP_LEVELS, DEFAULT_DP_LEVELS));
    }
    public List<String> getPreparedLevels() {
        return parseList(getStringValue(KEY_PREPARED_LEVELS, DEFAULT_PREPARED_LEVELS));
    }
    public List<String> getAptitudes() {
        return parseList(getStringValue(KEY_APTITUDES, DEFAULT_APTITUDES));
    }
    public boolean isPublicAccess() {
        return Boolean.parseBoolean(getStringValue(KEY_PUBLIC_ACCESS, "true"));
    }
    public boolean isSelfRegistration() {
        return Boolean.parseBoolean(getStringValue(KEY_SELF_REGISTRATION, "true"));
    }
    public boolean isMaintenanceMode() {
        return Boolean.parseBoolean(getStringValue(KEY_MAINTENANCE_MODE, "false"));
    }
    /** Types de créneaux qui bloquent tout chevauchement (liste vide = aucun type exclusif) */
    public List<String> getExclusiveSlotTypes() {
        return parseList(getStringValue(KEY_EXCLUSIVE_SLOT_TYPES, ""));
    }

    /** Durée par défaut d'un créneau à la création (en heures) */
    public int getDefaultSlotHours() {
        return getIntValue(KEY_DEFAULT_SLOT_HOURS, 2);
    }

    /** -1 = pas de restriction d'heure d'ouverture */
    public int getBookingOpenHour() {
        return getIntValueWithNegative(KEY_BOOKING_OPEN_HOUR, -1);
    }
    /** -1 = pas de restriction d'heure de fermeture */
    public int getBookingCloseHour() {
        return getIntValueWithNegative(KEY_BOOKING_CLOSE_HOUR, -1);
    }

    /** Adresse email de notification pour les nouvelles réservations (vide = pas d'envoi) */
    public String getNotificationBookingEmail() {
        return getStringValue(KEY_NOTIFICATION_BOOKING_EMAIL, "");
    }

    /** Durée maximale (en mois) pour la récurrence d'un créneau */
    public int getMaxRecurringMonths() {
        return getIntValue(KEY_MAX_RECURRING_MONTHS, 4);
    }

    // -- Getters notifications globales --
    public boolean isNotifRegistrationEnabled() {
        return Boolean.parseBoolean(getStringValue(KEY_NOTIF_REGISTRATION, "true"));
    }
    public boolean isNotifApprovedEnabled() {
        return Boolean.parseBoolean(getStringValue(KEY_NOTIF_APPROVED, "true"));
    }
    public boolean isNotifCancelledEnabled() {
        return Boolean.parseBoolean(getStringValue(KEY_NOTIF_CANCELLED, "true"));
    }
    public boolean isNotifMovedToWlEnabled() {
        return Boolean.parseBoolean(getStringValue(KEY_NOTIF_MOVED_TO_WL, "true"));
    }
    public boolean isNotifDpNewRegEnabled() {
        return Boolean.parseBoolean(getStringValue(KEY_NOTIF_DP_NEW_REG, "true"));
    }
    public boolean isNotifSafetyReminderEnabled() {
        return Boolean.parseBoolean(getStringValue(KEY_NOTIF_SAFETY_REMINDER, "false"));
    }
    public int getSafetyReminderDelayDays() {
        return getIntValue(KEY_SAFETY_REMINDER_DELAY_DAYS, 3);
    }
    public String getSafetyReminderEmailBody() {
        return getStringValue(KEY_SAFETY_REMINDER_EMAIL_BODY, DEFAULT_SAFETY_REMINDER_BODY);
    }
    /** Retourne la date d'activation du rappel, ou {@code null} si non définie. */
    public LocalDate getSafetyReminderActivationDate() {
        String raw = getStringValue(KEY_SAFETY_REMINDER_ACTIVATION_DATE, "");
        if (raw == null || raw.isBlank()) return null;
        try { return LocalDate.parse(raw); } catch (Exception ignored) { return null; }
    }

    // -- Getters rapport périodique --
    public boolean isReportEmailEnabled() {
        return Boolean.parseBoolean(getStringValue(KEY_REPORT_EMAIL_ENABLED, "false"));
    }
    public int getReportEmailPeriodDays() {
        return getIntValue(KEY_REPORT_EMAIL_PERIOD_DAYS, 7);
    }
    public String getReportEmailRecipients() {
        return getStringValue(KEY_REPORT_EMAIL_RECIPIENTS, "");
    }
    public String getReportEmailLastSent() {
        return getStringValue(KEY_REPORT_EMAIL_LAST_SENT, "");
    }

    public String getDefaultOrganizerMailTemplate() {
        return getStringValue(KEY_DEFAULT_ORGANIZER_MAIL_TEMPLATE, "");
    }

    public String getSafetySheetNotificationEmails() {
        return getStringValue(KEY_SAFETY_SHEET_NOTIFICATION_EMAILS, "");
    }

    public String getSafetySheetViewerEmails() {
        return getStringValue(KEY_SAFETY_SHEET_VIEWER_EMAILS, "");
    }

    public ConfigResponse getConfig() {
        return new ConfigResponse(
                getMaxDivers(), getSlotMinHours(), getSlotMaxHours(),
                getSlotResolutionMinutes(), getSiteName(),
                getSlotTypes(), getClubs(), getLevels(),
                getDiverLevels(), getDpLevels(), getPreparedLevels(),
                getAptitudes(),
                isPublicAccess(), isSelfRegistration(),
                getBookingOpenHour(), getBookingCloseHour(),
                getExclusiveSlotTypes(), getDefaultSlotHours(),
                getNotificationBookingEmail(),
                getMaxRecurringMonths(),
                isNotifRegistrationEnabled(),
                isNotifApprovedEnabled(),
                isNotifCancelledEnabled(),
                isNotifMovedToWlEnabled(),
                isNotifDpNewRegEnabled(),
                isNotifSafetyReminderEnabled(),
                getSafetyReminderDelayDays(),
                getSafetyReminderEmailBody(),
                getSafetyReminderActivationDate() != null ? getSafetyReminderActivationDate().toString() : "",
                isMaintenanceMode(),
                isReportEmailEnabled(),
                getReportEmailPeriodDays(),
                getReportEmailRecipients(),
                getReportEmailLastSent(),
                getDefaultOrganizerMailTemplate(),
                getSafetySheetNotificationEmails(),
                getSafetySheetViewerEmails()
        );
    }

    // ---- Mise à jour admin ----

    @Transactional
    public ConfigResponse updateMaxDivers(int maxDivers) {
        forceUpsert(KEY_MAX_DIVERS, String.valueOf(maxDivers));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateSiteName(String siteName) {
        forceUpsert(KEY_SITE_NAME, siteName);
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateSlotTypes(List<String> types) {
        forceUpsert(KEY_SLOT_TYPES, serializeList(types));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateClubs(List<String> clubs) {
        forceUpsert(KEY_CLUBS, serializeList(clubs));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateLevels(List<String> levels) {
        forceUpsert(KEY_LEVELS, serializeList(levels));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateDiverLevels(List<String> levels) {
        forceUpsert(KEY_DIVER_LEVELS, serializeList(levels));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateDpLevels(List<String> levels) {
        forceUpsert(KEY_DP_LEVELS, serializeList(levels));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updatePreparedLevels(List<String> levels) {
        forceUpsert(KEY_PREPARED_LEVELS, serializeList(levels));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateAptitudes(List<String> aptitudes) {
        forceUpsert(KEY_APTITUDES, serializeList(aptitudes));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updatePublicAccess(boolean value) {
        forceUpsert(KEY_PUBLIC_ACCESS, String.valueOf(value));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateSelfRegistration(boolean value) {
        forceUpsert(KEY_SELF_REGISTRATION, String.valueOf(value));
        return getConfig();
    }
    @Transactional
    public ConfigResponse updateMaintenanceMode(boolean value) {
        forceUpsert(KEY_MAINTENANCE_MODE, String.valueOf(value));
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateReportEmailSettings(boolean enabled, int periodDays, String recipients) {
        forceUpsert(KEY_REPORT_EMAIL_ENABLED,     String.valueOf(enabled));
        forceUpsert(KEY_REPORT_EMAIL_PERIOD_DAYS, String.valueOf(periodDays));
        forceUpsert(KEY_REPORT_EMAIL_RECIPIENTS,  recipients != null ? recipients.trim() : "");
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateDefaultOrganizerMailTemplate(String template) {
        forceUpsert(KEY_DEFAULT_ORGANIZER_MAIL_TEMPLATE, template != null ? template : "");
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateSafetySheetConfig(String notificationEmails, String viewerEmails) {
        forceUpsert(KEY_SAFETY_SHEET_NOTIFICATION_EMAILS, notificationEmails != null ? notificationEmails.trim() : "");
        forceUpsert(KEY_SAFETY_SHEET_VIEWER_EMAILS,       viewerEmails       != null ? viewerEmails.trim()       : "");
        return getConfig();
    }

    @Transactional
    public void updateReportEmailLastSent(String isoDateTime) {
        forceUpsert(KEY_REPORT_EMAIL_LAST_SENT, isoDateTime);
    }
    @Transactional
    public ConfigResponse updateBookingHours(int openHour, int closeHour) {
        forceUpsert(KEY_BOOKING_OPEN_HOUR,  String.valueOf(openHour));
        forceUpsert(KEY_BOOKING_CLOSE_HOUR, String.valueOf(closeHour));
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateExclusiveSlotTypes(List<String> types) {
        forceUpsert(KEY_EXCLUSIVE_SLOT_TYPES, serializeList(types));
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateDefaultSlotHours(int hours) {
        forceUpsert(KEY_DEFAULT_SLOT_HOURS, String.valueOf(hours));
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateSlotMaxHours(int hours) {
        if (hours < getSlotMinHours())
            throw new jakarta.ws.rs.BadRequestException(
                    "La durée maximale doit être supérieure ou égale à la durée minimale (" + getSlotMinHours() + "h)");
        forceUpsert(KEY_MAX_HOURS, String.valueOf(hours));
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateNotificationBookingEmail(String email) {
        forceUpsert(KEY_NOTIFICATION_BOOKING_EMAIL, email != null ? email.trim() : "");
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateMaxRecurringMonths(int months) {
        forceUpsert(KEY_MAX_RECURRING_MONTHS, String.valueOf(months));
        return getConfig();
    }

    @Transactional
    public ConfigResponse updateNotifSettings(
            boolean registration, boolean approved, boolean cancelled,
            boolean movedToWl, boolean dpNewReg,
            boolean safetyReminder, int safetyReminderDelayDays, String safetyReminderEmailBody,
            String safetyReminderActivationDate) {
        forceUpsert(KEY_NOTIF_REGISTRATION, String.valueOf(registration));
        forceUpsert(KEY_NOTIF_APPROVED,     String.valueOf(approved));
        forceUpsert(KEY_NOTIF_CANCELLED,    String.valueOf(cancelled));
        forceUpsert(KEY_NOTIF_MOVED_TO_WL,  String.valueOf(movedToWl));
        forceUpsert(KEY_NOTIF_DP_NEW_REG,   String.valueOf(dpNewReg));
        forceUpsert(KEY_NOTIF_SAFETY_REMINDER,      String.valueOf(safetyReminder));
        forceUpsert(KEY_SAFETY_REMINDER_DELAY_DAYS, String.valueOf(safetyReminderDelayDays));
        forceUpsert(KEY_SAFETY_REMINDER_EMAIL_BODY, safetyReminderEmailBody != null ? safetyReminderEmailBody.trim() : DEFAULT_SAFETY_REMINDER_BODY);
        // Si une date d'activation est fournie explicitement, on l'enregistre
        if (safetyReminderActivationDate != null && !safetyReminderActivationDate.isBlank()) {
            try {
                LocalDate.parse(safetyReminderActivationDate.trim()); // validation format
                forceUpsert(KEY_SAFETY_REMINDER_ACTIVATION_DATE, safetyReminderActivationDate.trim());
            } catch (Exception ignored) {}
        } else if (safetyReminder && getSafetyReminderActivationDate() == null) {
            // Auto-set uniquement si aucune date n'est encore définie et que le rappel est activé
            forceUpsert(KEY_SAFETY_REMINDER_ACTIVATION_DATE, LocalDate.now().toString());
        }
        return getConfig();
    }

    // ---- Init au démarrage ----

    @Transactional
    public void ensureDefaults() {
        if (divingConfig.maxDivers() > 0)
            forceUpsert(KEY_MAX_DIVERS, String.valueOf(divingConfig.maxDivers()));
        if (divingConfig.slotMinHours() > 0)
            forceUpsert(KEY_MIN_HOURS, String.valueOf(divingConfig.slotMinHours()));
        if (divingConfig.slotMaxHours() > 0)
            forceUpsert(KEY_MAX_HOURS, String.valueOf(divingConfig.slotMaxHours()));
        if (divingConfig.slotResolutionMinutes() > 0)
            forceUpsert(KEY_RESOLUTION, String.valueOf(divingConfig.slotResolutionMinutes()));
        if (divingConfig.siteName() != null && !divingConfig.siteName().isBlank())
            upsertIfMissing(KEY_SITE_NAME, divingConfig.siteName());

        upsertIfInvalid(KEY_MAX_DIVERS, "25");
        upsertIfInvalid(KEY_MIN_HOURS,  "1");
        upsertIfInvalid(KEY_MAX_HOURS,  "10");
        upsertIfInvalid(KEY_RESOLUTION, "15");
        upsertIfMissing(KEY_SITE_NAME,  "Carrière de Saint-Lin");
        upsertIfMissing(KEY_SLOT_TYPES,       DEFAULT_SLOT_TYPES);
        upsertIfMissing(KEY_CLUBS,             DEFAULT_CLUBS);
        upsertIfMissing(KEY_LEVELS,            DEFAULT_LEVELS);
        upsertIfMissing(KEY_DIVER_LEVELS,      DEFAULT_DIVER_LEVELS);
        upsertIfMissing(KEY_DP_LEVELS,         DEFAULT_DP_LEVELS);
        upsertIfMissing(KEY_PREPARED_LEVELS,   DEFAULT_PREPARED_LEVELS);
        upsertIfMissing(KEY_APTITUDES,         DEFAULT_APTITUDES);
        upsertIfMissing(KEY_PUBLIC_ACCESS,     "true");
        upsertIfMissing(KEY_SELF_REGISTRATION, "true");
        upsertIfMissing(KEY_BOOKING_OPEN_HOUR,    "-1");
        upsertIfMissing(KEY_BOOKING_CLOSE_HOUR,   "-1");
        upsertIfMissing(KEY_EXCLUSIVE_SLOT_TYPES, "");
        upsertIfMissing(KEY_DEFAULT_SLOT_HOURS,   "2");
        upsertIfMissing(KEY_NOTIFICATION_BOOKING_EMAIL, "");
        upsertIfMissing(KEY_MAX_RECURRING_MONTHS, "4");
        upsertIfMissing(KEY_NOTIF_REGISTRATION, "true");
        upsertIfMissing(KEY_NOTIF_APPROVED,     "true");
        upsertIfMissing(KEY_NOTIF_CANCELLED,    "true");
        upsertIfMissing(KEY_NOTIF_MOVED_TO_WL,  "true");
        upsertIfMissing(KEY_NOTIF_DP_NEW_REG,   "true");
        upsertIfMissing(KEY_NOTIF_SAFETY_REMINDER,           "false");
        upsertIfMissing(KEY_SAFETY_REMINDER_DELAY_DAYS,        "3");
        upsertIfMissing(KEY_SAFETY_REMINDER_EMAIL_BODY,        DEFAULT_SAFETY_REMINDER_BODY);
        upsertIfMissing(KEY_SAFETY_REMINDER_ACTIVATION_DATE,   "");
        upsertIfMissing(KEY_MAINTENANCE_MODE,           "false");
        upsertIfMissing(KEY_REPORT_EMAIL_ENABLED,       "false");
        upsertIfMissing(KEY_REPORT_EMAIL_PERIOD_DAYS,   "7");
        upsertIfMissing(KEY_REPORT_EMAIL_RECIPIENTS,    "");
        upsertIfMissing(KEY_REPORT_EMAIL_LAST_SENT,     "");
        upsertIfMissing(KEY_DEFAULT_ORGANIZER_MAIL_TEMPLATE, "");
        upsertIfMissing(KEY_SAFETY_SHEET_NOTIFICATION_EMAILS, "");
        upsertIfMissing(KEY_SAFETY_SHEET_VIEWER_EMAILS,       "");
    }

    // ---- Helpers privés ----

    private List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private String serializeList(List<String> items) {
        if (items == null) return "";
        return items.stream().map(String::trim).filter(s -> !s.isBlank())
                .collect(Collectors.joining("|"));
    }

    private int getIntValue(String key, int defaultVal) {
        AppConfigEntry entry = AppConfigEntry.findByKey(key);
        if (entry != null && entry.configValue != null) {
            try {
                int val = Integer.parseInt(entry.configValue);
                if (val > 0) return val;
            } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    /** Comme getIntValue mais accepte les valeurs négatives (ex: -1 = désactivé). */
    private int getIntValueWithNegative(String key, int defaultVal) {
        AppConfigEntry entry = AppConfigEntry.findByKey(key);
        if (entry != null && entry.configValue != null) {
            try {
                return Integer.parseInt(entry.configValue);
            } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private String getStringValue(String key, String defaultVal) {
        AppConfigEntry entry = AppConfigEntry.findByKey(key);
        if (entry != null && entry.configValue != null && !entry.configValue.isBlank()) {
            return entry.configValue;
        }
        return defaultVal;
    }

    private void forceUpsert(String key, String value) {
        AppConfigEntry entry = AppConfigEntry.findByKey(key);
        if (entry == null) {
            entry = new AppConfigEntry();
            entry.configKey = key;
        }
        entry.configValue = value;
        entry.updatedAt = LocalDateTime.now();
        entry.persist();
    }

    private void upsertIfInvalid(String key, String defaultValue) {
        AppConfigEntry entry = AppConfigEntry.findByKey(key);
        if (entry == null) {
            entry = new AppConfigEntry();
            entry.configKey = key;
            entry.configValue = defaultValue;
            entry.updatedAt = LocalDateTime.now();
            entry.persist();
        } else if (entry.configValue == null || entry.configValue.isBlank() || "0".equals(entry.configValue.trim())) {
            entry.configValue = defaultValue;
            entry.updatedAt = LocalDateTime.now();
            entry.persist();
        }
    }

    private void upsertIfMissing(String key, String defaultValue) {
        AppConfigEntry entry = AppConfigEntry.findByKey(key);
        if (entry == null) {
            entry = new AppConfigEntry();
            entry.configKey = key;
            entry.configValue = defaultValue;
            entry.updatedAt = LocalDateTime.now();
            entry.persist();
        }
    }
}
