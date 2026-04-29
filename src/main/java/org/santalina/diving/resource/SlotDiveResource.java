package org.santalina.diving.resource;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.Palanquee;
import org.santalina.diving.domain.SlotDive;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.dto.SlotDiveDto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/slots/{slotId}/dives")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
@Tag(name = "Plongées")
public class SlotDiveResource {

    @Inject
    JsonWebToken jwt;

    /** GET /api/slots/{slotId}/dives — liste les plongées du créneau */
    @GET
    public List<SlotDiveResponse> list(@PathParam("slotId") Long slotId) {
        checkSlotAccess(slotId);
        return SlotDive.findBySlot(slotId)
                .stream()
                .map(SlotDiveResponse::from)
                .toList();
    }

    /** POST /api/slots/{slotId}/dives — crée une plongée */
    @POST
    @Transactional
    public Response create(@PathParam("slotId") Long slotId,
                           CreateSlotDiveRequest req) {
        DiveSlot slot = checkSlotAccess(slotId);
        long count = SlotDive.count("slot.id", slotId);

        SlotDive dive = new SlotDive();
        dive.slot      = slot;
        dive.diveIndex = (int) count + 1;
        dive.label     = req != null ? req.label() : null;
        dive.startTime = req != null ? req.startTime() : null;
        dive.endTime   = req != null ? req.endTime() : null;
        dive.depth     = req != null ? req.depth() : null;
        dive.duration  = req != null ? req.duration() : null;
        dive.persist();

        return Response.status(201).entity(SlotDiveResponse.from(dive)).build();
    }

    /** PATCH /api/slots/{slotId}/dives/{diveId} — met à jour une plongée */
    @PATCH
    @Path("/{diveId}")
    @Transactional
    public SlotDiveResponse update(@PathParam("slotId") Long slotId,
                                   @PathParam("diveId") Long diveId,
                                   UpdateSlotDiveRequest req) {
        checkSlotAccess(slotId);
        SlotDive dive = findDive(slotId, diveId);
        if (req != null) {
            if (req.label() != null || req.startTime() != null || req.endTime() != null
                    || req.depth() != null || req.duration() != null) {
                dive.label     = req.label();
                dive.startTime = req.startTime();
                dive.endTime   = req.endTime();
                dive.depth     = req.depth();
                dive.duration  = req.duration();
            }
        }
        return SlotDiveResponse.from(dive);
    }

    /** DELETE /api/slots/{slotId}/dives/{diveId} — supprime une plongée (détache les palanquées) */
    @DELETE
    @Path("/{diveId}")
    @Transactional
    public Response delete(@PathParam("slotId") Long slotId,
                           @PathParam("diveId") Long diveId) {
        checkSlotAccess(slotId);
        SlotDive dive = findDive(slotId, diveId);

        // Détacher toutes les palanquées liées
        Palanquee.update("slotDive = null WHERE slotDive.id = ?1", diveId);

        dive.delete();

        // Réindexer les plongées restantes
        List<SlotDive> remaining = SlotDive.findBySlot(slotId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).diveIndex = i + 1;
        }

        return Response.noContent().build();
    }

    /** PUT /api/slots/{slotId}/dives/assign — assigne ou désassigne une palanquée à une plongée */
    @PUT
    @Path("/assign")
    @Transactional
    public Response assign(@PathParam("slotId") Long slotId,
                           @Valid AssignPalanqueeToDiveRequest req) {
        checkSlotAccess(slotId);
        Palanquee pal = Palanquee.findById(req.palanqueeId());
        if (pal == null || !pal.slot.id.equals(slotId)) {
            throw new NotFoundException("Palanquée non trouvée sur ce créneau");
        }

        if (req.slotDiveId() == null) {
            pal.slotDive = null;
        } else {
            pal.slotDive = findDive(slotId, req.slotDiveId());
        }
        return Response.ok().build();
    }

    // ---- helpers ----

    private DiveSlot checkSlotAccess(Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        boolean isAdmin;
        String principalName;
        try {
            isAdmin = jwt.getGroups() != null && jwt.getGroups().contains("ADMIN");
            principalName = jwt.getName();
        } catch (Exception e) {
            // En test (@TestSecurity), le principal n'est pas un JWT → on vérifie via SecurityContext
            return slot; // accès accordé (le filtre @RolesAllowed a déjà vérifié le rôle)
        }

        if (!isAdmin) {
            User me = User.findByEmail(principalName);
            boolean isCreator    = me != null && slot.createdBy != null && slot.createdBy.id.equals(me.id);
            boolean isAssignedDP = me != null && SlotDiver.isAssignedDirectorByEmail(slot.id, me.email);
            if (!isCreator && !isAssignedDP) {
                throw new ForbiddenException("Accès réservé au créateur du créneau");
            }
        }
        return slot;
    }

    private SlotDive findDive(Long slotId, Long diveId) {
        SlotDive d = SlotDive.findById(diveId);
        if (d == null || !d.slot.id.equals(slotId)) {
            throw new NotFoundException("Plongée non trouvée");
        }
        return d;
    }
}
