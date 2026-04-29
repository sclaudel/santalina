package org.santalina.diving.resource;

import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.RegistrationStatus;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.WaitingListEntry;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverRequest;
import org.santalina.diving.dto.WaitingListDto.StatusUpdateRequest;
import org.santalina.diving.dto.WaitingListDto.WaitingListRequest;
import org.santalina.diving.dto.WaitingListDto.WaitingListResponse;
import org.santalina.diving.mail.WaitingListMailer;
import org.santalina.diving.service.DelayedNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.santalina.diving.security.NameUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestion de la liste d'attente d'un créneau :
 * <ul>
 *   <li>POST  — inscription d'un plongeur authentifié (DIVER)</li>
 *   <li>GET   — lecture de la liste (DP du créneau ou ADMIN)</li>
 *   <li>POST  /{entryId}/approve — validation → transfert dans slot_divers</li>
 *   <li>PATCH /{entryId}/status — mise à jour du statut de vérification (DP/ADMIN)</li>
 *   <li>DELETE/{entryId} — annulation par le plongeur lui-même ou par le DP/ADMIN</li>
 * </ul>
 */
@Path("/api/slots/{slotId}/waiting-list")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Liste d'attente")
public class WaitingListResource {

    private static final Logger LOG = Logger.getLogger(WaitingListResource.class);

    /** Extensions de fichier autorisées pour les pièces jointes. */
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "pdf", "webp");
    /** Taille maximale d'un fichier joint : 5 Mo. */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Inject SecurityIdentity identity;
    @Inject WaitingListMailer mailer;
    @Inject DelayedNotificationService delayedNotif;
    @Inject DivingConfig divingConfig;
    @Inject ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Inscription (plongeur authentifié)  — multipart quand pièces jointes requises
    // -------------------------------------------------------------------------

    /**
     * Inscription JSON simple (créneau sans pièces jointes obligatoires).
     */
    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"DIVER", "DIVE_DIRECTOR", "ADMIN"})
    public Response register(@PathParam("slotId") Long slotId,
                             @Valid WaitingListRequest request) {
        return doRegister(slotId, request, null, null);
    }

    /**
     * Inscription multipart (créneau avec pièces jointes obligatoires).
     * Le champ {@code data} contient le JSON de {@link WaitingListRequest}.
     */
    @POST
    @Path("/with-attachments")
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({"DIVER", "DIVE_DIRECTOR", "ADMIN"})
    public Response registerWithAttachments(
            @PathParam("slotId") Long slotId,
            @RestForm("data")    String dataJson,
            @RestForm("medicalCert") FileUpload medicalCert,
            @RestForm("licenseQr")   FileUpload licenseQr) throws IOException {

        WaitingListRequest request;
        try {
            request = objectMapper.readValue(dataJson, WaitingListRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Le champ 'data' n'est pas un JSON valide : " + e.getMessage());
        }

        validateUpload(medicalCert, "certificat médical");
        validateUpload(licenseQr,   "QR code de la licence");

        return doRegister(slotId, request, medicalCert, licenseQr);
    }

    private Response doRegister(Long slotId, WaitingListRequest request,
                                 FileUpload medicalCert, FileUpload licenseQr) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        if (!SlotDiver.hasDirector(slotId)) {
            throw new BadRequestException("Ce créneau n'a pas encore de directeur de plongée — les inscriptions libres ne sont pas disponibles");
        }
        if (!slot.registrationOpen) {
            throw new BadRequestException("Les inscriptions libres ne sont pas activées sur ce créneau");
        }
        if (slot.registrationOpensAt != null && LocalDateTime.now().isBefore(slot.registrationOpensAt)) {
            throw new BadRequestException("Les inscriptions ouvrent le " + slot.registrationOpensAt.toLocalDate()
                    + " à " + slot.registrationOpensAt.toLocalTime());
        }

        // Vérifier les pièces jointes si requises
        if (slot.requiresAttachments && (medicalCert == null || licenseQr == null)) {
            throw new BadRequestException("Ce créneau exige le certificat médical et le QR code de la licence FFESSM");
        }

        if (!Boolean.TRUE.equals(request.licenseConfirmed())) {
            throw new BadRequestException("Vous devez confirmer la validité de votre licence FFESSM");
        }
        if (request.medicalCertDate() == null) {
            throw new BadRequestException("La date de début de votre certificat médical est obligatoire");
        }
        if (request.medicalCertDate().isBefore(slot.slotDate.minusYears(1))) {
            throw new BadRequestException("Votre certificat médical est expiré (plus d'un an avant la date du créneau)");
        }
        if (!request.email().equalsIgnoreCase(request.emailConfirm())) {
            throw new BadRequestException("Les deux adresses e-mail ne correspondent pas");
        }
        if (WaitingListEntry.existsBySlotAndEmail(slotId, request.email())) {
            throw new BadRequestException("Vous êtes déjà inscrit(e) sur la liste d'attente de ce créneau");
        }
        if (existsDiverByEmail(slotId, request.email())) {
            throw new BadRequestException("Vous êtes déjà inscrit(e) comme plongeur sur ce créneau");
        }

        WaitingListEntry entry = new WaitingListEntry();
        entry.slot             = slot;
        entry.firstName        = NameUtil.capitalize(request.firstName().trim());
        entry.lastName         = request.lastName().trim().toUpperCase();
        entry.email            = request.email().trim().toLowerCase();
        entry.level            = request.level();
        entry.numberOfDives    = request.numberOfDives();
        entry.lastDiveDate     = request.lastDiveDate();
        entry.preparedLevel    = request.preparedLevel();
        entry.comment          = request.comment();
        entry.medicalCertDate  = request.medicalCertDate();
        entry.licenseConfirmed = Boolean.TRUE.equals(request.licenseConfirmed());
        entry.club             = request.club();
        entry.persist();  // flush pour obtenir l'id avant de stocker les fichiers

        // Persister les pièces jointes si fournies
        if (medicalCert != null) {
            String path = saveAttachmentFile(medicalCert, slotId, entry.id, "medical_cert");
            entry.medicalCertPath = path;
        }
        if (licenseQr != null) {
            String path = saveAttachmentFile(licenseQr, slotId, entry.id, "license_qr");
            entry.licenseQrPath = path;
        }
        if (medicalCert != null || licenseQr != null) {
            entry.persist();
        }

        mailer.sendWaitingListConfirmation(entry, slot);
        for (OwnerRef owner : resolveOwnerRefs(slot)) {
            mailer.sendNewRegistrationToDP(entry, slot, owner.email(), owner.isAssignedDp());
        }

        return Response.status(201).entity(WaitingListResponse.from(entry)).build();
    }

    // -------------------------------------------------------------------------
    // Lecture de la liste (DP du créneau ou ADMIN)
    // -------------------------------------------------------------------------

    @GET
    @Path("/me")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"DIVER", "DIVE_DIRECTOR", "ADMIN"})
    public Response getMyEntry(@PathParam("slotId") Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        String callerEmail = identity.getPrincipal().getName();
        WaitingListEntry entry = WaitingListEntry.findBySlotAndEmail(slotId, callerEmail);
        if (entry == null) {
            return Response.status(404).build();
        }
        return Response.ok(WaitingListResponse.from(entry)).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public List<WaitingListResponse> getWaitingList(@PathParam("slotId") Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        requireSlotOwnerOrAdmin(slot);

        return WaitingListEntry.findBySlotOrdered(slotId).stream()
                .map(WaitingListResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Mise à jour du statut de vérification (DP / ADMIN)
    // -------------------------------------------------------------------------

    @PATCH
    @Path("/{entryId}/status")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response updateStatus(@PathParam("slotId") Long slotId,
                                 @PathParam("entryId") Long entryId,
                                 @Valid StatusUpdateRequest request) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");
        requireSlotOwnerOrAdmin(slot);

        WaitingListEntry entry = WaitingListEntry.findById(entryId);
        if (entry == null || !entry.slot.id.equals(slotId)) {
            throw new NotFoundException("Entrée non trouvée dans la liste d'attente");
        }

        if (request.status() == RegistrationStatus.INCOMPLETE) {
            // Envoyer l'email AVANT de supprimer l'entrée (pour avoir accès aux données)
            mailer.sendRegistrationIncomplete(entry, slot, request.reason());
            // Supprimer les fichiers joints
            deleteAttachmentFiles(entry);
            // Supprimer l'entrée — le plongeur devra se réinscrire
            entry.delete();
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        entry.registrationStatus = request.status();
        entry.rejectionReason    = request.reason();
        entry.persist();

        if (request.status() == RegistrationStatus.VERIFIED) {
            mailer.sendRegistrationVerified(entry, slot);
        }

        return Response.ok(WaitingListResponse.from(entry)).build();
    }

    // -------------------------------------------------------------------------
    // Validation (DP uniquement) — transfert vers slot_divers
    // -------------------------------------------------------------------------

    @POST
    @Path("/{entryId}/approve")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response approve(@PathParam("slotId") Long slotId,
                            @PathParam("entryId") Long entryId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        requireSlotOwnerOrAdmin(slot);

        WaitingListEntry entry = WaitingListEntry.findById(entryId);
        if (entry == null || !entry.slot.id.equals(slotId)) {
            throw new NotFoundException("Entrée non trouvée dans la liste d'attente");
        }

        long current = SlotDiver.countBySlot(slotId);
        if (current >= slot.diverCount) {
            throw new BadRequestException("Le créneau est complet (" + slot.diverCount + " plongeurs max)");
        }
        if (SlotDiver.existsBySlotAndName(slotId, entry.firstName, entry.lastName)) {
            throw new BadRequestException("Un plongeur avec ce nom est déjà inscrit sur ce créneau");
        }

        SlotDiver diver = new SlotDiver();
        diver.slot           = slot;
        diver.firstName      = entry.firstName;
        diver.lastName       = entry.lastName;
        diver.level          = entry.level;
        diver.email          = entry.email;
        diver.isDirector     = false;
        diver.aptitudes      = null;
        diver.medicalCertDate = entry.medicalCertDate;
        diver.comment        = entry.comment;
        diver.club           = entry.club;
        diver.persist();

        delayedNotif.scheduleApprovedMail(slotId, entry.email, entry.firstName, entry.lastName);

        // Supprimer les fichiers attachés avant de supprimer l'entrée
        deleteAttachmentFiles(entry);
        entry.delete();

        return Response.ok(org.santalina.diving.dto.SlotDiverDto.SlotDiverResponse.from(diver)).build();
    }

    // -------------------------------------------------------------------------
    // Annulation (plongeur par son email, ou DP/ADMIN)
    // -------------------------------------------------------------------------

    @DELETE
    @Path("/{entryId}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"DIVER", "DIVE_DIRECTOR", "ADMIN"})
    public Response cancel(@PathParam("slotId") Long slotId,
                           @PathParam("entryId") Long entryId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        WaitingListEntry entry = WaitingListEntry.findById(entryId);
        if (entry == null || !entry.slot.id.equals(slotId)) {
            throw new NotFoundException("Entrée non trouvée dans la liste d'attente");
        }

        String callerEmail = identity.getPrincipal().getName();
        String role        = getRole();

        boolean isAdmin      = "ADMIN".equals(role);
        boolean isSlotOwner  = "DIVE_DIRECTOR".equals(role)
                && slot.createdBy != null
                && slot.createdBy.email.equalsIgnoreCase(callerEmail);
        boolean isDiver      = entry.email.equalsIgnoreCase(callerEmail);

        if (!isAdmin && !isSlotOwner && !isDiver) {
            throw new ForbiddenException("Vous n'êtes pas autorisé(e) à annuler cette inscription");
        }

        if (isDiver && !isAdmin && !isSlotOwner) {
            for (OwnerRef owner : resolveOwnerRefs(slot)) {
                mailer.sendCancellationToDP(entry, slot, owner.email(), owner.isAssignedDp());
            }
        }
        if ((isAdmin || isSlotOwner) && !isDiver) {
            if (entry.email != null && !entry.email.isBlank()) {
                mailer.sendCancellationToDiver(entry.email, entry.firstName, entry.lastName, slot);
            }
        }

        deleteAttachmentFiles(entry);
        entry.delete();
        return Response.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateUpload(FileUpload upload, String label) {
        if (upload == null || upload.uploadedFile() == null) {
            throw new BadRequestException("Le fichier '" + label + "' est manquant");
        }
        if (upload.size() > MAX_FILE_SIZE) {
            throw new BadRequestException("Le fichier '" + label + "' dépasse la taille maximale autorisée (5 Mo)");
        }
        String ext = getExtension(upload.fileName());
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BadRequestException("Le fichier '" + label + "' doit être une image (JPG, PNG, WEBP) ou un PDF");
        }
    }

    /**
     * Enregistre un fichier uploadé et retourne son chemin relatif au répertoire data.
     * Chemin : {@code attachments/waiting-list/{slotId}/{entryId}/{baseName}.{ext}}
     */
    private String saveAttachmentFile(FileUpload upload, Long slotId, Long entryId, String baseName) {
        String ext = getExtension(upload.fileName());
        String relativePath = "attachments/waiting-list/" + slotId + "/" + entryId + "/" + baseName + "." + ext;
        java.nio.file.Path target = Paths.get(divingConfig.dataDir()).resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = Files.newInputStream(upload.uploadedFile())) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.errorf(e, "Impossible de sauvegarder le fichier %s", target);
            throw new InternalServerErrorException("Erreur lors de la sauvegarde du fichier joint");
        }
        return relativePath;
    }

    /** Supprime les fichiers attachés d'une entrée de la liste d'attente. */
    private void deleteAttachmentFiles(WaitingListEntry entry) {
        deleteFileIfExists(entry.medicalCertPath);
        deleteFileIfExists(entry.licenseQrPath);
        // Essayer de supprimer le répertoire parent s'il est vide
        if (entry.medicalCertPath != null) {
            try {
                java.nio.file.Path dir = Paths.get(divingConfig.dataDir())
                        .resolve(entry.medicalCertPath).getParent();
                if (dir != null && Files.isDirectory(dir)) {
                    try (var stream = Files.list(dir)) {
                        if (stream.findAny().isEmpty()) Files.delete(dir);
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    private void deleteFileIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            java.nio.file.Path p = Paths.get(divingConfig.dataDir()).resolve(relativePath);
            Files.deleteIfExists(p);
        } catch (IOException e) {
            LOG.warnf("Impossible de supprimer le fichier %s : %s", relativePath, e.getMessage());
        }
    }

    private static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }

    private void requireSlotOwnerOrAdmin(DiveSlot slot) {
        String role = getRole();
        if ("ADMIN".equals(role)) return;
        if ("DIVE_DIRECTOR".equals(role)) {
            User current = User.findByEmail(identity.getPrincipal().getName());
            if (current != null) {
                boolean isCreator    = slot.createdBy != null && slot.createdBy.id.equals(current.id);
                boolean isAssignedDP = SlotDiver.isAssignedDirectorByEmail(slot.id, current.email);
                if (isCreator || isAssignedDP) return;
            }
        }
        throw new ForbiddenException("Accès réservé au directeur de plongée du créneau");
    }

    private String getRole() {
        var roles = identity.getRoles();
        return (roles != null && !roles.isEmpty()) ? roles.iterator().next() : "";
    }

    private record OwnerRef(String email, boolean isAssignedDp) {}

    private List<OwnerRef> resolveOwnerRefs(DiveSlot slot) {
        List<OwnerRef> owners = new ArrayList<>();
        SlotDiver dp = SlotDiver.<SlotDiver>find("slot.id = ?1 and isDirector = true", slot.id).firstResult();
        String dpEmail = (dp != null && dp.email != null && !dp.email.isBlank())
                ? dp.email.toLowerCase() : null;
        if (dpEmail != null) owners.add(new OwnerRef(dpEmail, true));

        if (slot.createdBy != null && slot.createdBy.email != null && !slot.createdBy.email.isBlank()) {
            String creatorEmail = slot.createdBy.email.toLowerCase();
            if (!creatorEmail.equals(dpEmail)) owners.add(new OwnerRef(creatorEmail, false));
        }
        return owners;
    }

    private boolean existsDiverByEmail(Long slotId, String email) {
        return SlotDiver.count("slot.id = ?1 and lower(email) = lower(?2)", slotId, email) > 0;
    }
}
