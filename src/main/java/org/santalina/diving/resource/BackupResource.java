package org.santalina.diving.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.santalina.diving.dto.BackupDto.*;
import org.santalina.diving.service.BackupService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;

@Path("/api/admin/backup")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Administration - Sauvegarde")
public class BackupResource {

    private static final Logger LOG = Logger.getLogger(BackupResource.class);

    @Inject
    BackupService backupService;

    /**
     * Exporte la configuration et les utilisateurs (sans créneaux).
     */
    @GET
    @Path("/export/config-users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportConfigUsers() {
        BackupData data = backupService.exportConfigUsers();
        String filename = "santalina-config-users-" + LocalDate.now() + ".json";
        return Response.ok(data)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Exporte toutes les données (config + utilisateurs + créneaux + plongeurs).
     */
    @GET
    @Path("/export/full")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportFull() {
        BackupData data = backupService.exportFull();
        String filename = "santalina-full-" + LocalDate.now() + ".json";
        return Response.ok(data)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Exporte toutes les pièces jointes (fiches de sécurité) sous forme d'un ZIP.
     * Le ZIP contient un {@code manifest.json} et les fichiers organisés par créneau.
     */
    @GET
    @Path("/export/attachments")
    @Produces("application/zip")
    public Response exportAttachments() {
        String filename = "santalina-attachments-" + LocalDate.now() + ".zip";
        return Response.ok((StreamingOutput) out -> {
            try {
                backupService.exportAttachmentsZip(out);
            } catch (IOException e) {
                LOG.errorf(e, "Erreur lors de l'export des pièces jointes");
                throw new InternalServerErrorException("Erreur lors de l'export des pièces jointes");
            }
        })
        .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
        .build();
    }

    /**
     * Importe un ZIP de pièces jointes généré par {@code export/attachments}.
     * Recrée les fichiers physiques et les enregistrements en base.
     * ⚠️ À effectuer après un import de sauvegarde JSON pour restaurer les fiches.
     */
    @POST
    @Path("/import/attachments")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importAttachments(@RestForm("file") FileUpload file) {
        if (file == null || file.uploadedFile() == null) {
            return Response.status(400)
                    .entity(new AttachmentsImportResult(false, "Fichier ZIP manquant", 0, 0))
                    .build();
        }
        try (InputStream in = Files.newInputStream(file.uploadedFile())) {
            AttachmentsImportResult result = backupService.importAttachmentsZip(in);
            return Response.ok(result).build();
        } catch (IOException e) {
            LOG.errorf(e, "Erreur lors de l'import des pièces jointes");
            return Response.status(500)
                    .entity(new AttachmentsImportResult(false, "Erreur : " + e.getMessage(), 0, 0))
                    .build();
        }
    }

    /**
     * Importe une sauvegarde en vidant d'abord toutes les données existantes.
     * ⚠️  Opération irréversible — à utiliser avec précaution.
     */
    @POST
    @Path("/import")
    public Response importBackup(BackupData backup) {
        if (backup == null) {
            return Response.status(400)
                    .entity(new ImportResult(false, "Données de sauvegarde manquantes", 0, 0, 0, 0, 0, 0, 0))
                    .build();
        }
        try {
            LOG.warnf("Import de sauvegarde demandé par un admin (type=%s)", backup.type());
            ImportResult result = backupService.importBackup(backup);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.errorf(e, "Erreur lors de l'import de sauvegarde");
            return Response.status(500)
                    .entity(new ImportResult(false, "Erreur : " + e.getMessage(), 0, 0, 0, 0, 0, 0, 0))
                    .build();
        }
    }
}

