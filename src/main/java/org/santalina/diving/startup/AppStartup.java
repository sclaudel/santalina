package org.santalina.diving.startup;

import org.santalina.diving.service.AuthService;
import org.santalina.diving.service.ConfigService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AppStartup {

    private static final Logger LOG = Logger.getLogger(AppStartup.class);

    @Inject
    AuthService authService;

    @Inject
    ConfigService configService;

    void onStart(@Observes StartupEvent event) {
        LOG.info("🌊 Démarrage de l'application...");
        configService.ensureDefaults();
        authService.ensureAdminExists();
        LOG.info("✅ Initialisation terminée");
    }
}
