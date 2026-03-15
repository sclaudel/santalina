package org.santalina.diving.resource;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.Palanquee;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.dto.PalanqueeDto.*;
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

@Path("/api/slots/{slotId}/palanquees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
@Tag(name = "Palanquées")
public class PalanqueeResource {

    @Inject
    JsonWebToken jwt;

    /** GET /api/slots/{slotId}/palanquees — liste les palanquées avec leurs plongeurs */
    @GET
    public List<PalanqueeResponse> list(@PathParam("slotId") Long slotId) {
        checkSlotAccess(slotId);
        return Palanquee.findBySlot(slotId).stream()
                .map(PalanqueeResponse::from)
                .toList();
    }

    /** POST /api/slots/{slotId}/palanquees — crée une palanquée */
    @POST
    @Transactional
    public Response create(@PathParam("slotId") Long slotId,
                           @Valid CreatePalanqueeRequest req) {
        DiveSlot slot = checkSlotAccess(slotId);
        long count = Palanquee.count("slot.id", slotId);

        Palanquee p = new Palanquee();
        p.slot     = slot;
        p.name     = req.name();
        p.position = (int) count;
        p.persist();

        return Response.status(201).entity(PalanqueeResponse.from(p)).build();
    }

    /** PUT /api/slots/{slotId}/palanquees/{id} — renommer */
    @PUT
    @Path("/{id}")
    @Transactional
    public PalanqueeResponse rename(@PathParam("slotId") Long slotId,
                                    @PathParam("id") Long id,
                                    @Valid RenamePalanqueeRequest req) {
        checkSlotAccess(slotId);
        Palanquee p = findPalanquee(slotId, id);
        p.name     = req.name();
        p.depth    = req.depth();
        p.duration = req.duration();
        return PalanqueeResponse.from(p);
    }

    /** DELETE /api/slots/{slotId}/palanquees/{id} — supprime (désassigne les plongeurs) */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("slotId") Long slotId,
                           @PathParam("id") Long id) {
        checkSlotAccess(slotId);
        Palanquee p = findPalanquee(slotId, id);

        // Désassigner tous les plongeurs de cette palanquée
        SlotDiver.update("palanquee = null where palanquee.id = ?1", id);

        p.delete();
        return Response.noContent().build();
    }

    /** PUT /api/slots/{slotId}/palanquees/assign — assigne ou désassigne un plongeur */
    @PUT
    @Path("/assign")
    @Transactional
    public Response assign(@PathParam("slotId") Long slotId,
                           @Valid AssignDiverRequest req) {
        checkSlotAccess(slotId);

        SlotDiver diver = SlotDiver.findById(req.diverId());
        if (diver == null || !diver.slot.id.equals(slotId)) {
            throw new NotFoundException("Plongeur non trouvé sur ce créneau");
        }

        if (req.palanqueeId() == null) {
            diver.palanquee = null;
        } else {
            Palanquee target = findPalanquee(slotId, req.palanqueeId());
            diver.palanquee = target;
        }

        return Response.ok().build();
    }

    /** PUT /api/slots/{slotId}/palanquees/{id}/reorder — réordonne les plongeurs */
    @PUT
    @Path("/{id}/reorder")
    @Transactional
    public Response reorder(@PathParam("slotId") Long slotId,
                            @PathParam("id") Long id,
                            @Valid ReorderRequest req) {
        checkSlotAccess(slotId);
        findPalanquee(slotId, id);
        List<Long> ids = req.diverIds();
        for (int i = 0; i < ids.size(); i++) {
            SlotDiver diver = SlotDiver.findById(ids.get(i));
            if (diver != null && diver.palanquee != null && diver.palanquee.id.equals(id)) {
                diver.palanqueePosition = i;
            }
        }
        return Response.ok().build();
    }

    // ---- helpers ----

    private DiveSlot checkSlotAccess(Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        String role = jwt.getGroups() != null && jwt.getGroups().contains("ADMIN")
                ? "ADMIN" : "DIVE_DIRECTOR";

        if ("DIVE_DIRECTOR".equals(role)) {
            User me = User.findByEmail(jwt.getName());
            if (me == null || slot.createdBy == null || !slot.createdBy.id.equals(me.id)) {
                throw new ForbiddenException("Accès réservé au créateur du créneau");
            }
        }
        return slot;
    }

    private Palanquee findPalanquee(Long slotId, Long palanqueeId) {
        Palanquee p = Palanquee.findById(palanqueeId);
        if (p == null || !p.slot.id.equals(slotId)) {
            throw new NotFoundException("Palanquée non trouvée");
        }
        return p;
    }
}
