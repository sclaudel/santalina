package org.santalina.diving.resource;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.Palanquee;
import org.santalina.diving.domain.PalanqueeMember;
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

        // La suppression cascade sur palanquee_members via ON DELETE CASCADE
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
            if (req.fromPalanqueeId() != null) {
                // Désassigner d'une palanquée spécifique (mode multi-plongée)
                PalanqueeMember.deleteByDiverAndPalanquee(diver.id, req.fromPalanqueeId());
            } else {
                // Désassigner de TOUTES les palanquées (mode classique)
                PalanqueeMember.deleteByDiver(diver.id);
            }
        } else {
            Palanquee target = findPalanquee(slotId, req.palanqueeId());
            // Upsert : ajouter seulement si pas déjà membre
            if (PalanqueeMember.findByDiverAndPalanquee(diver.id, req.palanqueeId()) == null) {
                PalanqueeMember m = new PalanqueeMember();
                m.palanquee = target;
                m.diver     = diver;
                m.position  = (int) PalanqueeMember.count("palanquee.id = ?1", req.palanqueeId());
                m.persist();
            }
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
            PalanqueeMember m = PalanqueeMember.findByDiverAndPalanquee(ids.get(i), id);
            if (m != null) {
                m.position = i;
            }
        }
        return Response.ok().build();
    }

    /** PATCH /api/slots/{slotId}/palanquees/{palanqueeId}/members/{diverId}/aptitudes — aptitudes spécifiques à une plongée */
    @PATCH
    @Path("/{palanqueeId}/members/{diverId}/aptitudes")
    @Transactional
    public Response updateMemberAptitudes(@PathParam("slotId") Long slotId,
                                          @PathParam("palanqueeId") Long palanqueeId,
                                          @PathParam("diverId") Long diverId,
                                          UpdateMemberAptitudesRequest req) {
        checkSlotAccess(slotId);
        PalanqueeMember member = PalanqueeMember.findByDiverAndPalanquee(diverId, palanqueeId);
        if (member == null) throw new NotFoundException("Membre non trouvé dans cette palanquée");
        member.aptitudes = (req != null && req.aptitudes() != null && !req.aptitudes().isBlank()) ? req.aptitudes() : null;
        return Response.noContent().build();
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
            // En test (@TestSecurity), le principal n'est pas un JWT → accès accordé
            return slot;
        }

        if (!isAdmin) {
            User me = User.findByEmail(principalName);
            boolean isCreator = me != null && slot.createdBy != null && slot.createdBy.id.equals(me.id);
            boolean isAssignedDP = me != null && SlotDiver.isAssignedDirectorByEmail(slot.id, me.email);
            if (!isCreator && !isAssignedDP) {
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
