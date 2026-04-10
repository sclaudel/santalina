package org.santalina.diving.resource;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverRequest;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Comparator;
import java.util.List;

@Path("/api/slots/{slotId}/divers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Plongeurs")
public class SlotDiverResource {

    @Inject
    JsonWebToken jwt;

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
            User currentUser = User.findByEmail(jwt.getName());
            boolean isCreator = currentUser != null && slot.createdBy != null && slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = currentUser != null && SlotDiver.isAssignedDirectorByEmail(slotId, currentUser.email);
            if (!isCreator && !isAssignedDP) {
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
        diver.firstName     = request.firstName().trim();
        diver.lastName      = request.lastName().trim().toUpperCase();
        diver.level         = request.level();
        diver.email         = request.email();
        diver.phone         = request.phone();
        diver.isDirector    = request.isDirector();
        diver.aptitudes     = request.aptitudes();
        diver.licenseNumber = request.licenseNumber();
        diver.persist();

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
            User currentUser = User.findByEmail(jwt.getName());
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

        diver.firstName     = request.firstName().trim();
        diver.lastName      = request.lastName().trim().toUpperCase();
        diver.level         = request.level();
        diver.email         = request.email();
        diver.phone         = request.phone();
        diver.isDirector    = request.isDirector();
        diver.aptitudes     = request.aptitudes();
        diver.licenseNumber = request.licenseNumber();
        diver.persist();

        return SlotDiverResponse.from(diver);
    }

    // DELETE /api/slots/{slotId}/divers/me — auto-désinscription (tout rôle authentifié)
    @DELETE
    @Path("/me")
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR", "DIVER"})
    public Response cancelMyRegistration(@PathParam("slotId") Long slotId) {
        String callerEmail = jwt.getName();
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
            User currentUser = User.findByEmail(jwt.getName());
            boolean isCreator = currentUser != null && diver.slot.createdBy != null && diver.slot.createdBy.id.equals(currentUser.id);
            boolean isAssignedDP = currentUser != null && SlotDiver.isAssignedDirectorByEmail(slotId, currentUser.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }
        diver.delete();
        return Response.noContent().build();
    }

    /** Retourne le rôle du JWT courant de façon sécurisée */
    private String getRole() {
        var groups = jwt.getGroups();
        return (groups != null && !groups.isEmpty()) ? groups.iterator().next() : "";
    }
}
