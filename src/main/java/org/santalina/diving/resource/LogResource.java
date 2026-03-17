package org.santalina.diving.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Path("/api/admin/logs")
@RolesAllowed("ADMIN")
@Tag(name = "Administration - Logs")
public class LogResource {

    private static final Logger LOG = Logger.getLogger(LogResource.class);

    @ConfigProperty(name = "app.logs.application.path",
            defaultValue = "/deployments/data/logs/santalina.log")
    String applicationLogPath;

    @ConfigProperty(name = "app.logs.nginx-access.path",
            defaultValue = "/deployments/data/logs/nginx-access.log")
    String nginxAccessLogPath;

    @ConfigProperty(name = "app.logs.nginx-error.path",
            defaultValue = "/deployments/data/logs/nginx-error.log")
    String nginxErrorLogPath;

    @ConfigProperty(name = "app.logs.smtp.path",
            defaultValue = "")
    Optional<String> smtpLogPath;

    /**
     * Retourne la liste des services de logs disponibles et leur état.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<LogInfo> listLogs() {
        List<LogInfo> result = new ArrayList<>();
        result.add(buildLogInfo("application", "Application (Quarkus)", applicationLogPath));
        result.add(buildLogInfo("nginx-access", "Nginx - Accès", nginxAccessLogPath));
        result.add(buildLogInfo("nginx-error", "Nginx - Erreurs", nginxErrorLogPath));
        if (smtpLogPath.isPresent() && !smtpLogPath.get().isBlank()) {
            result.add(buildLogInfo("smtp", "Serveur SMTP", smtpLogPath.get()));
        } else {
            result.add(new LogInfo("smtp", "Serveur SMTP", false, 0L,
                    "Non configuré — le serveur SMTP écrit ses logs en dehors de l'application"));
        }
        return result;
    }

    /**
     * Télécharge le fichier de log du service demandé (téléchargement complet).
     */
    @GET
    @Path("/{service}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadLog(@PathParam("service") String service) {
        String path = resolvePath(service);
        if (path == null || path.isBlank()) {
            return Response.status(404)
                    .entity("{\"message\":\"Service de log inconnu : " + service + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return Response.status(404)
                    .entity("{\"message\":\"Fichier de log introuvable : " + path + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        String filename = service + "-" + LocalDate.now() + ".log";
        StreamingOutput stream = output -> {
            try (InputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
        };

        LOG.infof("Téléchargement du log '%s' (%d octets)", service, file.length());
        return Response.ok(stream)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Length", file.length())
                .build();
    }

    /**
     * Retourne les N dernières lignes du log (aperçu).
     */
    @GET
    @Path("/{service}/tail")
    @Produces(MediaType.TEXT_PLAIN)
    public Response tailLog(@PathParam("service") String service,
                             @QueryParam("lines") @DefaultValue("200") int lines) {
        String path = resolvePath(service);
        if (path == null || path.isBlank()) {
            return Response.status(404).entity("Service de log inconnu : " + service).build();
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return Response.status(404).entity("Fichier de log introuvable : " + path).build();
        }

        try {
            String content = readLastLines(file, Math.min(lines, 5000));
            return Response.ok(content).build();
        } catch (IOException e) {
            LOG.errorf(e, "Erreur de lecture du log '%s'", service);
            return Response.serverError().entity("Erreur de lecture : " + e.getMessage()).build();
        }
    }

    // ---- Helpers ----

    private String resolvePath(String service) {
        return switch (service) {
            case "application"  -> applicationLogPath;
            case "nginx-access" -> nginxAccessLogPath;
            case "nginx-error"  -> nginxErrorLogPath;
            case "smtp"         -> smtpLogPath.filter(p -> !p.isBlank()).orElse(null);
            default             -> null;
        };
    }

    private LogInfo buildLogInfo(String id, String label, String path) {
        if (path == null || path.isBlank()) {
            return new LogInfo(id, label, false, 0L, "Chemin non configuré");
        }
        File file = new File(path);
        if (!file.exists()) {
            return new LogInfo(id, label, false, 0L, "Fichier absent : " + path);
        }
        return new LogInfo(id, label, true, file.length(), path);
    }

    /** Lit les N dernières lignes d'un fichier texte efficacement. */
    private String readLastLines(File file, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > maxLines) {
                    lines.remove(0);
                }
            }
        }
        return String.join("\n", lines);
    }

    public record LogInfo(
            String id,
            String label,
            boolean available,
            long sizeBytes,
            String info
    ) {}
}


