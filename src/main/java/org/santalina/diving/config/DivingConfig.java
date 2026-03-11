package org.santalina.diving.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "app.diving")
public interface DivingConfig {

    @WithName("max-divers")
    @WithDefault("25")
    int maxDivers();

    @WithName("site-name")
    @WithDefault("Carrière de Saint-Lin")
    String siteName();

    @WithName("slot.min-hours")
    @WithDefault("1")
    int slotMinHours();

    @WithName("slot.max-hours")
    @WithDefault("10")
    int slotMaxHours();

    @WithName("slot.resolution-minutes")
    @WithDefault("15")
    int slotResolutionMinutes();

    @WithName("reset-token.expiry-minutes")
    @WithDefault("30")
    int resetTokenExpiryMinutes();

    @WithName("jwt.expiry-hours")
    @WithDefault("24")
    int jwtExpiryHours();

    @WithName("admin.email")
    @WithDefault("admin@santalina.com")
    String adminEmail();

    @WithName("admin.password")
    @WithDefault("Admin1234")
    String adminPassword();

    @WithName("base-url")
    @WithDefault("http://localhost:8085")
    String baseUrl();
}
