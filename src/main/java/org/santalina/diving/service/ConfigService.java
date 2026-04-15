package org.santalina.diving.service;

import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.AppConfigEntry;
import org.santalina.diving.dto.ConfigDto.ConfigResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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
    private static final String KEY_NOTIF_SAFETY_REMINDER      = "notif.safety_reminder.enabled";
    private static final String KEY_SAFETY_REMINDER_DELAY_DAYS = "notif.safety_reminder.delay_days";
    private static final String KEY_SAFETY_REMINDER_EMAIL_BODY = "notif.safety_reminder.email_body";

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

    public ConfigResponse getConfig() {
        return new ConfigResponse(
                getMaxDivers(), getSlotMinHours(), getSlotMaxHours(),
                getSlotResolutionMinutes(), getSiteName(),
                getSlotTypes(), getClubs(), getLevels(),
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
                isMaintenanceMode()
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
            boolean safetyReminder, int safetyReminderDelayDays, String safetyReminderEmailBody) {
        forceUpsert(KEY_NOTIF_REGISTRATION, String.valueOf(registration));
        forceUpsert(KEY_NOTIF_APPROVED,     String.valueOf(approved));
        forceUpsert(KEY_NOTIF_CANCELLED,    String.valueOf(cancelled));
        forceUpsert(KEY_NOTIF_MOVED_TO_WL,  String.valueOf(movedToWl));
        forceUpsert(KEY_NOTIF_DP_NEW_REG,   String.valueOf(dpNewReg));
        forceUpsert(KEY_NOTIF_SAFETY_REMINDER,      String.valueOf(safetyReminder));
        forceUpsert(KEY_SAFETY_REMINDER_DELAY_DAYS, String.valueOf(safetyReminderDelayDays));
        forceUpsert(KEY_SAFETY_REMINDER_EMAIL_BODY, safetyReminderEmailBody != null ? safetyReminderEmailBody.trim() : DEFAULT_SAFETY_REMINDER_BODY);
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
        upsertIfMissing(KEY_NOTIF_SAFETY_REMINDER,      "false");
        upsertIfMissing(KEY_SAFETY_REMINDER_DELAY_DAYS, "3");
        upsertIfMissing(KEY_SAFETY_REMINDER_EMAIL_BODY, DEFAULT_SAFETY_REMINDER_BODY);
        upsertIfMissing(KEY_MAINTENANCE_MODE,           "false");
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
