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

    public ConfigResponse getConfig() {
        return new ConfigResponse(
                getMaxDivers(), getSlotMinHours(), getSlotMaxHours(),
                getSlotResolutionMinutes(), getSiteName(),
                getSlotTypes(), getClubs(), getLevels(),
                isPublicAccess(), isSelfRegistration()
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
