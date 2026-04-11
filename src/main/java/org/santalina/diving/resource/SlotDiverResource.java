package org.santalina.diving.resource;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.domain.WaitingListEntry;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverRequest;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverResponse;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Path("/api/slots/{slotId}/divers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Plongeurs")
public class SlotDiverResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    WaitingListMailer mailer;

    @Inject
    DelayedNotificationService delayedNotif;

    // GET /api/slots/{slotId}/divers — public
    @GET
    @PermitAll
    public List<SlotDiverResponse> getDivers(@PathParam("slotId") Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");
        return SlotDiver.findBySlot(slotId).stream()
                // Directeur en premier, puis ordre alphabétique
                .sorted(Comparator.<SlotDiver, Boolean>comparing(d -> !d.isDirector)
                        .thenComparing(d -> d.lastName))
                .map(SlotDiverResponse::from)
                .toList();
    }

    // POST /api/slots/{slotId}/divers — ADMIN ou DIVE_DIRECTOR
    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response addDiver(@PathParam("slotId") Long slotId, @Valid SlotDiverRequest request) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        // Vérifier droits directeur de plongée
        if ("DIVE_DIRECTOR".equals(getRole())) {
            User currentUser = User.findByEmail(identity.getPrincipal().getName());
            boolean isCreator = currentUser != null && slot.createdBy != null && slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = currentUser != null && SlotDiver.isAssignedDirectorByEmail(slotId, currentUser.email);
            // Autoriser l'auto-assignation : un DP peut se désigner lui-même comme directeur
            boolean isSelfAssigning = request.isDirector()
                    && request.email() != null
                    && currentUser != null
                    && request.email().equalsIgnoreCase(currentUser.email);
            if (!isCreator && !isAssignedDP && !isSelfAssigning) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }

        // Validation : champs obligatoires si directeur de plongée
        if (request.isDirector()) {
            if (request.email() == null || request.email().isBlank()) {
                throw new BadRequestException("L'email est obligatoire pour un directeur de plongée");
            }
            if (request.phone() == null || request.phone().isBlank()) {
                throw new BadRequestException("Le téléphone est obligatoire pour un directeur de plongée");
            }
            // Un seul directeur par créneau
            if (SlotDiver.hasDirector(slotId)) {
                throw new BadRequestException("Il y a déjà un directeur de plongée sur ce créneau");
            }
        }

        // Vérifier doublon nom/prénom sur le créneau
        if (SlotDiver.existsBySlotAndName(slotId, request.firstName(), request.lastName())) {
            throw new BadRequestException("Un plongeur avec ce nom et prénom est déjà inscrit sur ce créneau");
        }

        // Vérifier capacité
        long current = SlotDiver.countBySlot(slotId);
        if (current >= slot.diverCount) {
            throw new BadRequestException(
                    "Le créneau est complet (" + slot.diverCount + " plongeurs max)");
        }

        SlotDiver diver = new SlotDiver();
        diver.slot          = slot;
        diver.firstName     = NameUtil.capitalize(request.firstName().trim());
        diver.lastName      = request.lastName().trim().toUpperCase();
        diver.level         = request.level();
        diver.email         = request.email() != null ? request.email().trim().toLowerCase() : null;
        diver.phone         = request.phone();
        diver.isDirector    = request.isDirector();
        diver.aptitudes     = request.aptitudes();
        diver.licenseNumber = request.licenseNumber();
        diver.persist();

        // Notifier le DP et le créateur si un plongeur (non directeur) est ajouté
        if (!diver.isDirector) {
            String callerEmail = identity.getPrincipal().getName().toLowerCase();
            for (OwnerRef owner : resolveOwnerRefs(slot)) {
                if (!owner.email().equalsIgnoreCase(callerEmail)) {
                    mailer.sendNewSlotDiverToDP(diver, slot, owner.email(), owner.isAssignedDp());
                }
            }
        }

        return Response.status(201).entity(SlotDiverResponse.from(diver)).build();
    }

    // PUT /api/slots/{slotId}/divers/{diverId} — ADMIN ou DIVE_DIRECTOR
    @PUT
    @Path("/{diverId}")
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public SlotDiverResponse updateDiver(@PathParam("slotId") Long slotId,
                                         @PathParam("diverId") Long diverId,
                                         @Valid SlotDiverRequest request) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        SlotDiver diver = SlotDiver.findById(diverId);
        if (diver == null || !diver.slot.id.equals(slotId)) {
            throw new NotFoundException("Plongeur non trouvé");
        }

        // Vérifier droits directeur de plongée
        String role = getRole();
        if ("DIVE_DIRECTOR".equals(role)) {
            User currentUser = User.findByEmail(identity.getPrincipal().getName());
            boolean isCreator = currentUser != null && slot.createdBy != null && slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = currentUser != null && SlotDiver.isAssignedDirectorByEmail(slotId, currentUser.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }

        // Validation directeur
        if (request.isDirector()) {
            if (request.email() == null || request.email().isBlank())
                throw new BadRequestException("L'email est obligatoire pour un directeur de plongée");
            if (request.phone() == null || request.phone().isBlank())
                throw new BadRequestException("Le téléphone est obligatoire pour un directeur de plongée");
            // Si on change vers directeur et que ce n'était pas lui avant, vérifier unicité
            if (!diver.isDirector && SlotDiver.hasDirector(slotId))
                throw new BadRequestException("Il y a déjà un directeur de plongée sur ce créneau");
        }

        // Vérifier doublon nom/prénom sur le créneau (en excluant le plongeur courant)
        if (SlotDiver.existsBySlotAndNameExcluding(slotId, request.firstName(), request.lastName(), diverId)) {
            throw new BadRequestException("Un plongeur avec ce nom et prénom est déjà inscrit sur ce créneau");
        }

        diver.firstName     = NameUtil.capitalize(request.firstName().trim());
        diver.lastName      = request.lastName().trim().toUpperCase();
        diver.level         = request.level();
        diver.email         = request.email() != null ? request.email().trim().toLowerCase() : null;
        diver.phone         = request.phone();
        diver.isDirector    = request.isDirector();
        diver.aptitudes     = request.aptitudes();
        diver.licenseNumber = request.licenseNumber();
        diver.club          = request.club();
        diver.persist();

        return SlotDiverResponse.from(diver);
    }

    // DELETE /api/slots/{slotId}/divers/me — auto-désinscription (tout rôle authentifié)
    @DELETE
    @Path("/me")
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR", "DIVER"})
    public Response cancelMyRegistration(@PathParam("slotId") Long slotId) {
        String callerEmail = identity.getPrincipal().getName();
        SlotDiver diver = SlotDiver.findBySlotAndEmail(slotId, callerEmail);
        if (diver == null) {
            throw new NotFoundException("Vous n'êtes pas inscrit sur ce créneau");
        }
        if (diver.isDirector) {
            throw new BadRequestException("Le directeur de plongée assigné au créneau ne peut pas se désinscrire via cette voie");
        }
        diver.delete();
        return Response.noContent().build();
    }

    // DELETE /api/slots/{slotId}/divers/{diverId} — ADMIN ou DIVE_DIRECTOR
    @DELETE
    @Path("/{diverId}")
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response removeDiver(@PathParam("slotId") Long slotId,
                                @PathParam("diverId") Long diverId) {
        SlotDiver diver = SlotDiver.findById(diverId);
        if (diver == null || !diver.slot.id.equals(slotId)) {
            throw new NotFoundException("Plongeur non trouvé");
        }
        if ("DIVE_DIRECTOR".equals(getRole())) {
            User currentUser = User.findByEmail(identity.getPrincipal().getName());
            boolean isCreator = currentUser != null && diver.slot.createdBy != null && diver.slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = currentUser != null && SlotDiver.isAssignedDirectorByEmail(slotId, currentUser.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }
        // Notifier le plongeur si il a un e-mail
        if (diver.email != null && !diver.email.isBlank() && !diver.isDirector) {
            mailer.sendCancellationToDiver(diver.email, diver.firstName, diver.lastName, diver.slot);
        }
        diver.delete();
        return Response.noContent().build();
    }

    // POST /api/slots/{slotId}/divers/{diverId}/move-to-waiting-list
    // Réintègre un plongeur confirmé dans la liste d'attente (ADMIN ou DP du créneau)
    @POST
    @Path("/{diverId}/move-to-waiting-list")
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response moveToWaitingList(@PathParam("slotId") Long slotId,
                                      @PathParam("diverId") Long diverId) {
        SlotDiver diver = SlotDiver.findById(diverId);
        if (diver == null || !diver.slot.id.equals(slotId)) {
            throw new NotFoundException("Plongeur non trouvé");
        }
        if (diver.isDirector) {
            throw new BadRequestException("Le directeur de plongée ne peut pas être remis en liste d'attente");
        }

        DiveSlot slot = diver.slot;

        if ("DIVE_DIRECTOR".equals(getRole())) {
            User currentUser = User.findByEmail(identity.getPrincipal().getName());
            boolean isCreator = currentUser != null && slot.createdBy != null && slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = currentUser != null && SlotDiver.isAssignedDirectorByEmail(slotId, currentUser.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }

        // Vérifier que l'email n'est pas déjà en liste d'attente
        if (diver.email != null && WaitingListEntry.existsBySlotAndEmail(slotId, diver.email)) {
            throw new BadRequestException("Ce plongeur est déjà dans la liste d'attente");
        }

        // Créer l'entrée en liste d'attente
        WaitingListEntry entry = new WaitingListEntry();
        entry.slot      = slot;
        entry.firstName = diver.firstName;
        entry.lastName  = diver.lastName;
        entry.email     = diver.email != null ? diver.email.toLowerCase() : null;
        entry.level     = diver.level;
        entry.persist();

        // Supprimer de slot_divers
        diver.delete();

        // Planifier l'envoi du mail avec délai de grâce (15 min)
        if (entry.email != null && !entry.email.isBlank()) {
            delayedNotif.scheduleMovedToWlMail(
                    entry.id, slotId,
                    entry.email, entry.firstName, entry.lastName, entry.level);
        }

        return Response.ok(org.santalina.diving.dto.WaitingListDto.WaitingListResponse.from(entry)).build();
    }
    private String getRole() {
        var roles = identity.getRoles();
        return (roles != null && !roles.isEmpty()) ? roles.iterator().next() : "";
    }

    private record OwnerRef(String email, boolean isAssignedDp) {}

    /** Retourne les références uniques du DP assigné et du créateur du créneau. */
    private List<OwnerRef> resolveOwnerRefs(DiveSlot slot) {
        List<OwnerRef> owners = new ArrayList<>();
        SlotDiver dp = SlotDiver.<SlotDiver>find("slot.id = ?1 and isDirector = true", slot.id).firstResult();
        String dpEmail = (dp != null && dp.email != null && !dp.email.isBlank()) ? dp.email.toLowerCase() : null;
        if (dpEmail != null) owners.add(new OwnerRef(dpEmail, true));
        if (slot.createdBy != null && slot.createdBy.email != null && !slot.createdBy.email.isBlank()) {
            String creatorEmail = slot.createdBy.email.toLowerCase();
            if (!creatorEmail.equals(dpEmail)) owners.add(new OwnerRef(creatorEmail, false));
        }
        return owners;
    }
}
