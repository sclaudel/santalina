package org.santalina.diving.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.mail.DpOrganizerMailer;
import org.santalina.diving.service.ConfigService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Endpoint pour l'envoi du mail d'organisation du directeur de plongée.
 * POST /api/slots/{slotId}/mail/organization
 */
@Path("/api/slots/{slotId}/mail")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
@Tag(name = "Mail de sortie")
public class SlotMailResource {

    static final long MAX_ATTACHMENT_SIZE = 3 * 1024 * 1024L; // 3 Mo

    @Inject
    JsonWebToken jwt;

    @Inject
    DpOrganizerMailer dpOrganizerMailer;

    @Inject
    ConfigService configService;

    @Inject
    ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // DTO interne
    // ─────────────────────────────────────────────────────────────────────────

    public record SendOrganizationMailRequest(
            String subject,
            String htmlBody,
            Map<Long, String> emailOverrides
    ) {}

    public record MissingEmailInfo(Long diverId, String diverName) {}

    public record OrganizationMailResponse(int sent, List<MissingEmailInfo> missingEmails) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Endpoint
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/slots/{slotId}/mail/organization
     * <p>
     * Corps multipart :
     * <ul>
     *   <li>{@code data} — JSON de type {@link SendOrganizationMailRequest}</li>
     *   <li>{@code attachment} — fichier optionnel (max 3 Mo), supprimé après envoi</li>
     * </ul>
     * </p>
     */
    @POST
    @Path("/organization")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response sendOrganizationMail(
            @PathParam("slotId") Long slotId,
            @RestForm("data") String dataJson,
            @RestForm("attachment") FileUpload attachment) throws IOException {

        SendOrganizationMailRequest request;
        try {
            request = objectMapper.readValue(dataJson != null ? dataJson : "{}", SendOrganizationMailRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Payload JSON invalide : " + e.getMessage());
        }

        if (request.htmlBody() == null || request.htmlBody().isBlank()) {
            throw new BadRequestException("Le corps du mail est obligatoire");
        }
        if (request.subject() == null || request.subject().isBlank()) {
            throw new BadRequestException("L'objet du mail est obligatoire");
        }

        // Vérifier la taille de la pièce jointe
        java.io.File attachFile = null;
        String attachName = null;
        byte[] attachBytes = null;
        String attachMime = "application/octet-stream";
        if (attachment != null && attachment.fileName() != null && !attachment.fileName().isBlank()) {
            attachFile = attachment.uploadedFile().toFile();
            if (attachFile.length() > MAX_ATTACHMENT_SIZE) {
                throw new BadRequestException("La pièce jointe dépasse la taille maximale autorisée (3 Mo)");
            }
            attachName  = attachment.fileName();
            attachBytes = Files.readAllBytes(attachment.uploadedFile());
            if (attachment.contentType() != null && !attachment.contentType().isBlank()) {
                attachMime = attachment.contentType();
            }
        }

        DiveSlot slot = checkSlotAccess(slotId);
        User dp       = User.findByEmail(jwt.getName());
        List<SlotDiver> divers = SlotDiver.findBySlot(slotId);

        Map<Long, String> overrides = request.emailOverrides() != null ? request.emailOverrides() : Map.of();

        // Valider le format des emails saisis manuellement
        java.util.regex.Pattern emailPattern = java.util.regex.Pattern.compile(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
        for (Map.Entry<Long, String> entry : overrides.entrySet()) {
            String email = entry.getValue();
            if (email != null && !email.isBlank() && !emailPattern.matcher(email.trim()).matches()) {
                throw new BadRequestException("Format d'email invalide : " + email.trim());
            }
        }

        // Vérifier les emails manquants
        List<MissingEmailInfo> missing = new ArrayList<>();
        for (SlotDiver d : divers) {
            boolean hasEmail = (d.email != null && !d.email.isBlank())
                    || (overrides.containsKey(d.id) && !overrides.get(d.id).isBlank());
            if (!hasEmail) {
                missing.add(new MissingEmailInfo(d.id, d.firstName + " " + d.lastName));
            }
        }

        if (!missing.isEmpty()) {
            return Response.status(422)
                    .entity(new OrganizationMailResponse(0, missing))
                    .build();
        }

        String siteName = configService.getSiteName();
        try {
            dpOrganizerMailer.sendOrganizationEmail(slot, dp, divers, overrides,
                    request.subject(), request.htmlBody(), siteName,
                    attachName, attachBytes, attachMime);
        } finally {
            // Supprimer le fichier temporaire après envoi
            if (attachFile != null && attachFile.exists()) {
                attachFile.delete();
            }
        }

        // Compter les destinataires effectifs (emails uniques)
        long sent = divers.stream()
                .map(d -> (d.email != null && !d.email.isBlank()) ? d.email.trim()
                        : overrides.getOrDefault(d.id, ""))
                .filter(e -> !e.isBlank())
                .distinct()
                .count();

        return Response.ok(new OrganizationMailResponse((int) sent, List.of())).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private DiveSlot checkSlotAccess(Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        String role = jwt.getGroups() != null && jwt.getGroups().contains("ADMIN")
                ? "ADMIN" : "DIVE_DIRECTOR";

        if ("DIVE_DIRECTOR".equals(role)) {
            User me = User.findByEmail(jwt.getName());
            boolean isCreator    = me != null && slot.createdBy != null && slot.createdBy.id.equals(me.id);
            boolean isAssignedDP = me != null && SlotDiver.isAssignedDirectorByEmail(slot.id, me.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Accès réservé au créateur ou directeur de plongée du créneau");
            }
        }
        return slot;
    }
}
