package org.santalina.diving.startup;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Corrige les compteurs IDENTITY H2 qui peuvent se désynchroniser
 * après un arrêt brutal de la JVM (kill, Ctrl+C en dev, etc.).
 * Ne s'exécute que lorsque le datasource est H2.
 */
@ApplicationScoped
public class H2IdentityFixup {

    private static final Logger LOG = Logger.getLogger(H2IdentityFixup.class);

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "quarkus.datasource.db-kind")
    String dbKind;

    void onStart(@Observes StartupEvent ev) {
        if (!"h2".equals(dbKind)) return;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE IS_IDENTITY = TRUE AND TABLE_SCHEMA = 'PUBLIC'")) {
            while (rs.next()) {
                fixIdentity(conn, rs.getString("TABLE_NAME"), rs.getString("COLUMN_NAME"));
            }
        } catch (Exception e) {
            LOG.warn("H2 identity fixup — impossible de lire les colonnes IDENTITY", e);
        }
    }

    private void fixIdentity(Connection conn, String table, String column) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT COALESCE(MAX(\"" + column + "\"), 0) FROM \"" + table + "\"");
            if (rs.next()) {
                long next = rs.getLong(1) + 1;
                stmt.execute("ALTER TABLE \"" + table + "\" ALTER COLUMN \"" + column
                        + "\" RESTART WITH " + next);
                LOG.debugf("H2 identity fixup: %s.%s → RESTART WITH %d", table, column, next);
            }
        } catch (Exception e) {
            LOG.warnf("H2 identity fixup échoué pour %s.%s: %s", table, column, e.getMessage());
        }
    }
}
