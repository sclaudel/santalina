package org.santalina.diving.resource;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.WaitingListEntry;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverRequest;
import org.santalina.diving.dto.WaitingListDto.WaitingListRequest;
import org.santalina.diving.dto.WaitingListDto.WaitingListResponse;
import org.santalina.diving.mail.WaitingListMailer;
import org.santalina.diving.service.DelayedNotificationService;
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
import org.santalina.diving.security.NameUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestion de la liste d'attente d'un créneau :
 * <ul>
 *   <li>POST  — inscription d'un plongeur authentifié (DIVER)</li>
 *   <li>GET   — lecture de la liste (DP du créneau ou ADMIN)</li>
 *   <li>POST  /{entryId}/approve — validation → transfert dans slot_divers</li>
 *   <li>DELETE/{entryId} — annulation par le plongeur lui-même ou par le DP/ADMIN</li>
 * </ul>
 */
@Path("/api/slots/{slotId}/waiting-list")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Liste d'attente")
public class WaitingListResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    WaitingListMailer mailer;

    @Inject
    DelayedNotificationService delayedNotif;

    // -------------------------------------------------------------------------
    // Inscription (plongeur authentifié)
    // -------------------------------------------------------------------------

    @POST
    @Transactional
    @RolesAllowed({"DIVER", "DIVE_DIRECTOR", "ADMIN"})
    public Response register(@PathParam("slotId") Long slotId,
                             @Valid WaitingListRequest request) {

        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        // Vérifier que le DP a bien assigné un directeur de plongée sur le créneau
        if (!SlotDiver.hasDirector(slotId)) {
            throw new BadRequestException("Ce créneau n'a pas encore de directeur de plongée — les inscriptions libres ne sont pas disponibles");
        }

        // Vérifier que les inscriptions sont ouvertes
        if (!slot.registrationOpen) {
            throw new BadRequestException("Les inscriptions libres ne sont pas activées sur ce créneau");
        }

        // Vérifier la date d'ouverture
        if (slot.registrationOpensAt != null && LocalDateTime.now().isBefore(slot.registrationOpensAt)) {
            throw new BadRequestException("Les inscriptions ouvrent le " + slot.registrationOpensAt.toLocalDate()
                    + " à " + slot.registrationOpensAt.toLocalTime());
        }

        // Vérifier que le plongeur confirme sa licence FFESSM
        if (!Boolean.TRUE.equals(request.licenseConfirmed())) {
            throw new BadRequestException("Vous devez confirmer la validité de votre licence FFESSM");
        }

        // Vérifier la date du certificat médical
        if (request.medicalCertDate() == null) {
            throw new BadRequestException("La date de début de votre certificat médical est obligatoire");
        }
        if (request.medicalCertDate().isBefore(slot.slotDate.minusYears(1))) {
            throw new BadRequestException("Votre certificat médical est expiré (plus d'un an avant la date du créneau)");
        }

        // Vérifier la double saisie email
        if (!request.email().equalsIgnoreCase(request.emailConfirm())) {
            throw new BadRequestException("Les deux adresses e-mail ne correspondent pas");
        }

        // Vérifier qu'on n'est pas déjà dans la liste d'attente
        if (WaitingListEntry.existsBySlotAndEmail(slotId, request.email())) {
            throw new BadRequestException("Vous êtes déjà inscrit(e) sur la liste d'attente de ce créneau");
        }

        // Vérifier qu'on n'est pas déjà validé (slot_divers)
        if (existsDiverByEmail(slotId, request.email())) {
            throw new BadRequestException("Vous êtes déjà inscrit(e) comme plongeur sur ce créneau");
        }

        WaitingListEntry entry = new WaitingListEntry();
        entry.slot          = slot;
        entry.firstName     = NameUtil.capitalize(request.firstName().trim());
        entry.lastName      = request.lastName().trim().toUpperCase();
        entry.email         = request.email().trim().toLowerCase();
        entry.level         = request.level();
        entry.numberOfDives = request.numberOfDives();
        entry.lastDiveDate  = request.lastDiveDate();
        entry.preparedLevel = request.preparedLevel();
        entry.comment       = request.comment();
        entry.medicalCertDate  = request.medicalCertDate();
        entry.licenseConfirmed = Boolean.TRUE.equals(request.licenseConfirmed());
        entry.persist();

        mailer.sendWaitingListConfirmation(entry, slot);

        // Notifier le DP assigné et le créateur du créneau (séparément pour les préférences)
        for (OwnerRef owner : resolveOwnerRefs(slot)) {
            mailer.sendNewRegistrationToDP(entry, slot, owner.email(), owner.isAssignedDp());
        }

        return Response.status(201).entity(WaitingListResponse.from(entry)).build();
    }

    // -------------------------------------------------------------------------
    // Lecture de la liste (DP du créneau ou ADMIN)
    // -------------------------------------------------------------------------

    // GET /me — entrée de l'utilisateur connecté pour ce créneau (tout rôle authentifié)
    @GET
    @Path("/me")
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
    // -------------------------------------------------------------------------

    @GET
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
    // Validation (DP uniquement) — transfert vers slot_divers
    // -------------------------------------------------------------------------

    @POST
    @Path("/{entryId}/approve")
    @Transactional
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

        // Vérifier capacité
        long current = SlotDiver.countBySlot(slotId);
        if (current >= slot.diverCount) {
            throw new BadRequestException("Le créneau est complet (" + slot.diverCount + " plongeurs max)");
        }

        // Vérifier doublon nom
        if (SlotDiver.existsBySlotAndName(slotId, entry.firstName, entry.lastName)) {
            throw new BadRequestException("Un plongeur avec ce nom est déjà inscrit sur ce créneau");
        }

        // Transférer dans slot_divers
        SlotDiver diver = new SlotDiver();
        diver.slot          = slot;
        diver.firstName     = entry.firstName;
        diver.lastName      = entry.lastName;
        diver.level         = entry.level;
        diver.email         = entry.email;
        diver.isDirector    = false;
        diver.aptitudes     = null;  // preparedLevel est informatif uniquement, pas pré-rempli dans les aptitudes
        diver.medicalCertDate = entry.medicalCertDate;
        diver.comment        = entry.comment;
        diver.persist();

        // Planifier l'envoi du mail de validation avec le délai de grâce (15 min)
        delayedNotif.scheduleApprovedMail(slotId, entry.email, entry.firstName, entry.lastName);

        // Supprimer de la liste d'attente
        entry.delete();

        return Response.ok(org.santalina.diving.dto.SlotDiverDto.SlotDiverResponse.from(diver)).build();
    }

    // -------------------------------------------------------------------------
    // Annulation (plongeur par son email, ou DP/ADMIN)
    // -------------------------------------------------------------------------

    @DELETE
    @Path("/{entryId}")
    @Transactional
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

        boolean isAdmin = "ADMIN".equals(role);
        boolean isSlotOwner = "DIVE_DIRECTOR".equals(role)
                && slot.createdBy != null
                && slot.createdBy.email.equalsIgnoreCase(callerEmail);
        boolean isDiver = entry.email.equalsIgnoreCase(callerEmail);

        if (!isAdmin && !isSlotOwner && !isDiver) {
            throw new ForbiddenException("Vous n'êtes pas autorisé(e) à annuler cette inscription");
        }

        // Envoyer mail au DP et au créateur si c'est le plongeur qui annule
        if (isDiver && !isAdmin && !isSlotOwner) {
            for (OwnerRef owner : resolveOwnerRefs(slot)) {
                mailer.sendCancellationToDP(entry, slot, owner.email(), owner.isAssignedDp());
            }
        }

        // Envoyer mail au plongeur si c'est le DP ou l'admin qui annule
        if ((isAdmin || isSlotOwner) && !isDiver) {
            if (entry.email != null && !entry.email.isBlank()) {
                mailer.sendCancellationToDiver(entry.email, entry.firstName, entry.lastName, slot);
            }
        }

        entry.delete();
        return Response.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Vérifie que le caller est propriétaire du créneau ou son DP assigné ou ADMIN. Lève 403 sinon. */
    private void requireSlotOwnerOrAdmin(DiveSlot slot) {
        String role = getRole();
        if ("ADMIN".equals(role)) return;
        if ("DIVE_DIRECTOR".equals(role)) {
            User current = User.findByEmail(identity.getPrincipal().getName());
            if (current != null) {
                boolean isCreator = slot.createdBy != null && slot.createdBy.id.equals(current.id);
                boolean isAssignedDP = SlotDiver.isAssignedDirectorByEmail(slot.id, current.email);
                if (isCreator || isAssignedDP) return;
            }
        }
        throw new ForbiddenException("Accès réservé au directeur de plongée du créneau");
    }

    /** Retourne le rôle du JWT. */
    private String getRole() {
        var roles = identity.getRoles();
        return (roles != null && !roles.isEmpty()) ? roles.iterator().next() : "";
    }

    /** Référence à un propriétaire du créneau (DP assigné et/ou créateur) */
    private record OwnerRef(String email, boolean isAssignedDp) {}

    /** Retourne les propriétaires du créneau (DP assigné et créateur) avec leur rôle. */
    private List<OwnerRef> resolveOwnerRefs(DiveSlot slot) {
        List<OwnerRef> owners = new ArrayList<>();

        SlotDiver dp = SlotDiver.<SlotDiver>find("slot.id = ?1 and isDirector = true", slot.id).firstResult();
        String dpEmail = (dp != null && dp.email != null && !dp.email.isBlank())
                ? dp.email.toLowerCase() : null;
        if (dpEmail != null) {
            owners.add(new OwnerRef(dpEmail, true));
        }

        if (slot.createdBy != null && slot.createdBy.email != null && !slot.createdBy.email.isBlank()) {
            String creatorEmail = slot.createdBy.email.toLowerCase();
            if (!creatorEmail.equals(dpEmail)) {
                owners.add(new OwnerRef(creatorEmail, false));
            }
        }
        return owners;
    }

    /** Vérifie si un email est déjà présent dans slot_divers pour ce créneau. */
    private boolean existsDiverByEmail(Long slotId, String email) {
        return SlotDiver.count(
                "slot.id = ?1 and lower(email) = lower(?2)", slotId, email) > 0;
    }
}
