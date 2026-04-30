package org.santalina.diving.resource;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.SlotSafetySheet;
import org.santalina.diving.domain.User;
import org.santalina.diving.mail.SafetySheetMailer;
import org.santalina.diving.service.ConfigService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Endpoints pour la gestion des fiches de sécurité déposées sur un créneau passé.
 * <ul>
 *   <li>{@code POST   /api/slots/{slotId}/safety-sheets}        — Upload (max 4 fichiers, 3 Mo chacun)</li>
 *   <li>{@code GET    /api/slots/{slotId}/safety-sheets}        — Liste les fichiers</li>
 *   <li>{@code GET    /api/slots/{slotId}/safety-sheets/zip}    — Télécharger tous les fichiers en ZIP</li>
 *   <li>{@code DELETE /api/slots/{slotId}/safety-sheets/{id}}   — Supprimer un fichier (ADMIN)</li>
 * </ul>
 */
@Path("/api/slots/{slotId}/safety-sheets")
@Tag(name = "Fiches de sécurité")
public class SlotSafetySheetResource {

    private static final Logger LOG = Logger.getLogger(SlotSafetySheetResource.class);

    /** Taille maximale par fichier : 3 Mo */
    private static final long MAX_FILE_SIZE = 3L * 1024 * 1024;

    /** Nombre maximum de fichiers par créneau */
    private static final int MAX_FILES_PER_SLOT = 4;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    @Inject SecurityIdentity identity;
    @Inject DivingConfig     divingConfig;
    @Inject ConfigService    configService;
    @Inject SafetySheetMailer safetySheetMailer;

    // ── DTO de réponse ────────────────────────────────────────────────────────

    public record SafetySheetResponse(
            Long   id,
            String originalName,
            String contentType,
            long   fileSize,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime uploadedAt,
            String uploaderName
    ) {
        public static SafetySheetResponse from(SlotSafetySheet s) {
            return new SafetySheetResponse(
                    s.id,
                    s.originalName,
                    s.contentType,
                    s.fileSize,
                    s.uploadedAt,
                    s.uploader != null ? s.uploader.fullName() : null
            );
        }
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Upload jusqu'à 4 fichiers (images ou PDF, 3 Mo max chacun) sur un créneau passé.
     * Accessible au DP (créateur ou assigné) et aux admins.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response upload(
            @PathParam("slotId") Long slotId,
            @RestForm("file1") FileUpload file1,
            @RestForm("file2") FileUpload file2,
            @RestForm("file3") FileUpload file3,
            @RestForm("file4") FileUpload file4) {

        DiveSlot slot = requireSlot(slotId);
        requirePastSlot(slot);
        requireSlotAccess(slot);

        List<FileUpload> uploads = Arrays.stream(new FileUpload[]{file1, file2, file3, file4})
                .filter(f -> f != null && f.uploadedFile() != null)
                .toList();

        if (uploads.isEmpty()) {
            throw new BadRequestException("Aucun fichier fourni");
        }

        long existing = SlotSafetySheet.countBySlot(slotId);
        if (existing + uploads.size() > MAX_FILES_PER_SLOT) {
            throw new BadRequestException(
                    "Le créneau ne peut pas contenir plus de " + MAX_FILES_PER_SLOT +
                    " fichier(s). Il en contient déjà " + existing + ".");
        }

        for (FileUpload upload : uploads) {
            validateUpload(upload);
        }

        User uploader = User.findByEmail(identity.getPrincipal().getName());

        for (FileUpload upload : uploads) {
            saveSheet(slot, uploader, upload);
        }

        // Désactiver le rappel de fiche de sécurité pour ce créneau
        if (slot.reminderSentAt == null) {
            slot.reminderSentAt = LocalDateTime.now();
            slot.persist();
        }

        // Notification par e-mail
        safetySheetMailer.sendNotification(slot);

        List<SafetySheetResponse> result = SlotSafetySheet.findBySlot(slotId)
                .stream().map(SafetySheetResponse::from).toList();
        return Response.ok(result).build();
    }

    // ── Liste ─────────────────────────────────────────────────────────────────

    /**
     * Retourne la liste des fichiers déposés sur le créneau.
     * Accessible au DP, au créateur, aux admins et aux comptes de la liste blanche.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR", "DIVER"})
    public List<SafetySheetResponse> list(@PathParam("slotId") Long slotId) {
        DiveSlot slot = requireSlot(slotId);
        requireReadAccess(slot);
        return SlotSafetySheet.findBySlot(slotId)
                .stream().map(SafetySheetResponse::from).toList();
    }

    // ── Téléchargement ZIP ────────────────────────────────────────────────────

    /**
     * Retourne un ZIP contenant toutes les fiches de sécurité du créneau.
     */
    @GET
    @Path("/zip")
    @Produces("application/zip")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR", "DIVER"})
    public Response downloadZip(@PathParam("slotId") Long slotId) {
        DiveSlot slot = requireSlot(slotId);
        requireReadAccess(slot);

        List<SlotSafetySheet> sheets = SlotSafetySheet.findBySlot(slotId);
        if (sheets.isEmpty()) {
            throw new NotFoundException("Aucune fiche de sécurité disponible pour ce créneau");
        }

        String zipName = "fiches_securite_" + slot.slotDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".zip";

        return Response.ok((StreamingOutput) out -> buildZip(sheets, out))
                .header("Content-Disposition", "attachment; filename=\"" + zipName + "\"")
                .build();
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    /**
     * Supprime un fichier. Réservé aux admins.
     */
    @DELETE
    @Path("/{fileId}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response delete(
            @PathParam("slotId") Long slotId,
            @PathParam("fileId") Long fileId) {

        requireSlot(slotId);

        SlotSafetySheet sheet = SlotSafetySheet.findById(fileId);
        if (sheet == null || !sheet.slot.id.equals(slotId)) {
            throw new NotFoundException("Fiche de sécurité introuvable");
        }

        deleteFile(sheet.filePath);
        sheet.delete();

        return Response.noContent().build();
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private DiveSlot requireSlot(Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");
        return slot;
    }

    private void requirePastSlot(DiveSlot slot) {
        if (!slot.slotDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Les fiches de sécurité ne peuvent être déposées que sur des créneaux passés");
        }
    }

    /** Vérifie que l'utilisateur courant peut déposer des fichiers (DP ou admin). */
    private void requireSlotAccess(DiveSlot slot) {
        if (isAdmin()) return;
        User current = currentUser();
        if (current == null) throw new ForbiddenException("Accès refusé");
        boolean isCreator    = slot.createdBy != null && slot.createdBy.id.equals(current.id);
        boolean isAssignedDP = SlotDiver.isAssignedDirectorByEmail(slot.id, current.email);
        if (!isCreator && !isAssignedDP) {
            throw new ForbiddenException("Accès réservé au directeur de plongée du créneau");
        }
    }

    /** Vérifie que l'utilisateur courant peut lire les fichiers. */
    private void requireReadAccess(DiveSlot slot) {
        if (isAdmin()) return;
        User current = currentUser();
        if (current == null) throw new ForbiddenException("Accès refusé");

        // DP créateur ou assigné
        boolean isCreator    = slot.createdBy != null && slot.createdBy.id.equals(current.id);
        boolean isAssignedDP = SlotDiver.isAssignedDirectorByEmail(slot.id, current.email);
        if (isCreator || isAssignedDP) return;

        // Liste blanche des viewers
        String viewerEmails = configService.getSafetySheetViewerEmails();
        if (viewerEmails != null && !viewerEmails.isBlank()) {
            boolean inViewerList = Arrays.stream(viewerEmails.split("[,;]"))
                    .map(String::trim)
                    .anyMatch(e -> e.equalsIgnoreCase(current.email));
            if (inViewerList) return;
        }

        throw new ForbiddenException("Accès réservé au directeur de plongée, au créateur du créneau, ou aux personnes autorisées");
    }

    private void validateUpload(FileUpload upload) {
        if (upload.size() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    "Le fichier '" + upload.fileName() + "' dépasse la taille maximale autorisée (3 Mo)");
        }
        String ext = getExtension(upload.fileName());
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BadRequestException(
                    "Le fichier '" + upload.fileName() + "' doit être une image (JPG, PNG, WEBP) ou un PDF");
        }
    }

    private void saveSheet(DiveSlot slot, User uploader, FileUpload upload) {
        String ext          = getExtension(upload.fileName());
        String storedName   = UUID.randomUUID() + "." + ext;
        String relativePath = "attachments/safety-sheets/" + slot.id + "/" + storedName;
        java.nio.file.Path target = Paths.get(divingConfig.dataDir()).resolve(relativePath);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = Files.newInputStream(upload.uploadedFile())) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.errorf(e, "Impossible de sauvegarder le fichier %s", target);
            throw new InternalServerErrorException("Erreur lors de la sauvegarde de la fiche");
        }

        SlotSafetySheet sheet = new SlotSafetySheet();
        sheet.slot         = slot;
        sheet.uploader     = uploader;
        sheet.originalName = sanitizeFileName(upload.fileName());
        sheet.storedName   = storedName;
        sheet.filePath     = relativePath;
        sheet.contentType  = detectContentType(upload.fileName());
        sheet.fileSize     = upload.size();
        sheet.uploadedAt   = LocalDateTime.now();
        sheet.expiresAt    = sheet.uploadedAt.plusYears(1);
        sheet.persist();
    }

    private void buildZip(List<SlotSafetySheet> sheets, OutputStream out) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (SlotSafetySheet sheet : sheets) {
                java.nio.file.Path file = Paths.get(divingConfig.dataDir()).resolve(sheet.filePath);
                if (!Files.exists(file)) {
                    LOG.warnf("Fichier introuvable lors du ZIP : %s", file);
                    continue;
                }
                zip.putNextEntry(new ZipEntry(sheet.originalName));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            java.nio.file.Path p = Paths.get(divingConfig.dataDir()).resolve(relativePath);
            Files.deleteIfExists(p);
            // Supprimer le répertoire parent s'il est vide
            java.nio.file.Path parent = p.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                try (var stream = Files.list(parent)) {
                    if (stream.findAny().isEmpty()) Files.delete(parent);
                }
            }
        } catch (IOException e) {
            LOG.warnf("Impossible de supprimer le fichier %s : %s", relativePath, e.getMessage());
        }
    }

    private boolean isAdmin() {
        Set<String> roles = identity.getRoles();
        return roles != null && roles.contains("ADMIN");
    }

    private User currentUser() {
        return User.findByEmail(identity.getPrincipal().getName());
    }

    private static String getExtension(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "bin";
    }

    private static String detectContentType(String filename) {
        String ext = getExtension(filename);
        return switch (ext) {
            case "pdf"  -> "application/pdf";
            case "png"  -> "image/png";
            case "webp" -> "image/webp";
            default     -> "image/jpeg";
        };
    }

    /** Garde uniquement le nom de fichier sans chemin et nettoie les caractères dangereux. */
    private static String sanitizeFileName(String name) {
        if (name == null) return "fichier";
        // Garder uniquement le nom de fichier (sans chemin)
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String base = slash >= 0 ? name.substring(slash + 1) : name;
        // Remplacer les caractères non autorisés par _
        return base.replaceAll("[^a-zA-Z0-9._\\-() ]", "_");
    }

    @FunctionalInterface
    private interface StreamingOutput extends jakarta.ws.rs.core.StreamingOutput {}
}
