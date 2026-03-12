package org.santalina.diving;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Fallback SPA : toute route non-API sans extension de fichier est renvoyée vers index.html,
 * ce qui permet au routeur React de prendre en charge les URLs directes (ex: /reset-password).
 */
@ApplicationScoped
public class SpaRoutingFilter {

    @RouteFilter(400)
    void redirectToSpa(RoutingContext rc) {
        String path = rc.request().path();
        if (path.startsWith("/api/") || path.startsWith("/q/") || path.contains(".")) {
            rc.next();
            return;
        }
        rc.reroute("/index.html");
    }
}
