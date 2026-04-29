package org.santalina.diving.resource;

import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.WaitingListEntry;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Endpoint pour télécharger les pièces jointes (certificat médical, QR code)
 * associées à une entrée de la liste d'attente.
 * <p>
 * Accessible uniquement aux rôles {@code ADMIN} et {@code DIVE_DIRECTOR}
 * (et uniquement si le DP est créateur ou DP assigné du créneau).
 * </p>
 */
@Path("/api/attachments/{slotId}/{entryId}/{type}")
@Tag(name = "Pièces jointes")
public class AttachmentResource {

    private static final Logger LOG = Logger.getLogger(AttachmentResource.class);

    @Inject SecurityIdentity identity;
    @Inject DivingConfig    divingConfig;

    /**
     * Retourne le fichier de la pièce jointe demandée.
     *
     * @param slotId  identifiant du créneau
     * @param entryId identifiant de l'entrée dans la liste d'attente
     * @param type    {@code medical-cert} ou {@code license-qr}
     */
    @GET
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    @Produces(MediaType.WILDCARD)
    public Response download(@PathParam("slotId")  Long   slotId,
                             @PathParam("entryId") Long   entryId,
                             @PathParam("type")    String type) {

        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        requireSlotOwnerOrAdmin(slot);

        WaitingListEntry entry = WaitingListEntry.findById(entryId);
        if (entry == null || !entry.slot.id.equals(slotId)) {
            throw new NotFoundException("Entrée non trouvée dans la liste d'attente");
        }

        if (entry.attachmentsDeletedAt != null) {
            return Response.status(410)
                    .entity("{\"message\":\"Les pièces jointes de cette inscription ont été supprimées\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        String relativePath = switch (type) {
            case "medical-cert" -> entry.medicalCertPath;
            case "license-qr"   -> entry.licenseQrPath;
            default -> throw new NotFoundException("Type de pièce jointe inconnu : " + type);
        };

        if (relativePath == null || relativePath.isBlank()) {
            throw new NotFoundException("Aucune pièce jointe de ce type pour cette inscription");
        }

        java.nio.file.Path file = Paths.get(divingConfig.dataDir()).resolve(relativePath);
        if (!Files.exists(file)) {
            LOG.warnf("Fichier introuvable sur le disque : %s", file);
            throw new NotFoundException("Fichier introuvable");
        }

        String contentType = detectContentType(file);

        try {
            byte[] bytes = Files.readAllBytes(file);
            return Response.ok(bytes)
                    .header("Content-Type",        contentType)
                    .header("Content-Disposition", "inline; filename=\"" + file.getFileName() + "\"")
                    .build();
        } catch (IOException e) {
            LOG.errorf(e, "Erreur lors de la lecture du fichier %s", file);
            throw new InternalServerErrorException("Impossible de lire le fichier");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void requireSlotOwnerOrAdmin(DiveSlot slot) {
        var roles = identity.getRoles();
        String role = (roles != null && !roles.isEmpty()) ? roles.iterator().next() : "";
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

    private static String detectContentType(java.nio.file.Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf"))  return "application/pdf";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
