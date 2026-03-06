package com.example.diving.service;

import com.example.diving.config.DivingConfig;
import com.example.diving.domain.AppConfigEntry;
import com.example.diving.dto.ConfigDto.ConfigResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class ConfigService {

    private static final String KEY_MAX_DIVERS = "max.divers";
    private static final String KEY_MIN_HOURS  = "slot.min.hours";
    private static final String KEY_MAX_HOURS  = "slot.max.hours";
    private static final String KEY_RESOLUTION = "slot.resolution.minutes";

    @Inject
    DivingConfig divingConfig;

    // ---- Getters publics (lisent la base, avec fallback hardcodé) ----

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

    public ConfigResponse getConfig() {
        return new ConfigResponse(
                getMaxDivers(),
                getSlotMinHours(),
                getSlotMaxHours(),
                getSlotResolutionMinutes()
        );
    }

    // ---- Mise à jour admin ----

    @Transactional
    public ConfigResponse updateMaxDivers(int maxDivers) {
        forceUpsert(KEY_MAX_DIVERS, String.valueOf(maxDivers));
        return getConfig();
    }

    // ---- Init au démarrage ----

    @Transactional
    public void ensureDefaults() {
        // Écrire les valeurs de application.properties si elles sont valides
        if (divingConfig.maxDivers() > 0)
            forceUpsert(KEY_MAX_DIVERS, String.valueOf(divingConfig.maxDivers()));
        if (divingConfig.slotMinHours() > 0)
            forceUpsert(KEY_MIN_HOURS, String.valueOf(divingConfig.slotMinHours()));
        if (divingConfig.slotMaxHours() > 0)
            forceUpsert(KEY_MAX_HOURS, String.valueOf(divingConfig.slotMaxHours()));
        if (divingConfig.slotResolutionMinutes() > 0)
            forceUpsert(KEY_RESOLUTION, String.valueOf(divingConfig.slotResolutionMinutes()));

        // Fallback hardcodé si les valeurs sont toujours absentes ou à 0
        upsertIfInvalid(KEY_MAX_DIVERS, "25");
        upsertIfInvalid(KEY_MIN_HOURS,  "1");
        upsertIfInvalid(KEY_MAX_HOURS,  "10");
        upsertIfInvalid(KEY_RESOLUTION, "15");
    }

    // ---- Helpers privés ----

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
}
