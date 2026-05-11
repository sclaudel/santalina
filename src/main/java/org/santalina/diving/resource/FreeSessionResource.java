package org.santalina.diving.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.santalina.diving.domain.*;
import org.santalina.diving.dto.FreeSessionDto.*;
import org.santalina.diving.security.NameUtil;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Ressource REST pour les sessions de plongée libres (sans créneau calendaire).
 * <p>
 * Accessible aux rôles {@code ADMIN} et {@code DIVE_DIRECTOR}.
 * Chaque DP ne peut posséder que 15 sessions au maximum.
 * <p>
 * Endpoints :
 * <ul>
 *   <li>GET    /api/free-sessions                                             — liste ses sessions</li>
 *   <li>POST   /api/free-sessions                                             — créer</li>
 *   <li>PUT    /api/free-sessions/{id}                                        — modifier</li>
 *   <li>DELETE /api/free-sessions/{id}                                        — supprimer</li>
 *   <li>GET    /api/free-sessions/{id}/divers                                 — liste plongeurs</li>
 *   <li>POST   /api/free-sessions/{id}/divers                                 — ajouter</li>
 *   <li>PUT    /api/free-sessions/{id}/divers/{did}                           — modifier</li>
 *   <li>DELETE /api/free-sessions/{id}/divers/{did}                           — supprimer</li>
 *   <li>GET    /api/free-sessions/{id}/dives                                  — liste plongées</li>
 *   <li>POST   /api/free-sessions/{id}/dives                                  — créer plongée</li>
 *   <li>PATCH  /api/free-sessions/{id}/dives/{diveId}                         — modifier</li>
 *   <li>DELETE /api/free-sessions/{id}/dives/{diveId}                         — supprimer</li>
 *   <li>PUT    /api/free-sessions/{id}/dives/assign                           — assigner palanquée ↔ plongée</li>
 *   <li>GET    /api/free-sessions/{id}/palanquees                             — liste palanquées</li>
 *   <li>POST   /api/free-sessions/{id}/palanquees                             — créer</li>
 *   <li>PUT    /api/free-sessions/{id}/palanquees/{pid}                       — renommer</li>
 *   <li>DELETE /api/free-sessions/{id}/palanquees/{pid}                       — supprimer</li>
 *   <li>PUT    /api/free-sessions/{id}/palanquees/assign                      — assigner plongeur</li>
 *   <li>PUT    /api/free-sessions/{id}/palanquees/{pid}/reorder               — réordonner</li>
 *   <li>PATCH  /api/free-sessions/{id}/palanquees/{pid}/members/{did}/aptitudes — aptitudes</li>
 * </ul>
 */
@Path("/api/free-sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
@Tag(name = "Sessions libres")
public class FreeSessionResource {

    /** Nombre maximum de sessions libres par DP. */
    private static final int MAX_SESSIONS = 15;

    @Inject
    SecurityIdentity identity;

    // ════════════════════════════════════════════════════════════════════════
    // Sessions
    // ════════════════════════════════════════════════════════════════════════

    @GET
    public List<SessionResponse> listSessions() {
        User me = currentUser();
        return FreeDiveSession.findByOwner(me.id).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @POST
    @Transactional
    public Response createSession(@Valid CreateSessionRequest req) {
        User me = currentUser();
        if (FreeDiveSession.countByOwner(me.id) >= MAX_SESSIONS) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity("Limite de " + MAX_SESSIONS + " sessions libres atteinte")
                            .build());
        }
        FreeDiveSession s = new FreeDiveSession();
        s.owner     = me;
        s.label     = req.label();
        s.diveDate  = req.diveDate();
        s.startTime = req.startTime();
        s.notes     = req.notes();
        s.persist();
        return Response.status(201).entity(SessionResponse.from(s)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public SessionResponse updateSession(@PathParam("id") Long id,
                                         @Valid UpdateSessionRequest req) {
        FreeDiveSession s = checkAccess(id);
        s.label     = req.label();
        s.diveDate  = req.diveDate();
        s.startTime = req.startTime();
        s.notes     = req.notes();
        s.updatedAt = LocalDateTime.now();
        return SessionResponse.from(s);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteSession(@PathParam("id") Long id) {
        FreeDiveSession s = checkAccess(id);
        // Cascade en base via ON DELETE CASCADE sur toutes les tables filles
        s.delete();
        return Response.noContent().build();
    }

    /**
     * Copie une session en conservant uniquement les plongeurs.
     * La date/heure est fournie dans la requête (obligatoire).
     */
    @POST
    @Path("/{id}/copy")
    @Transactional
    public Response copySession(@PathParam("id") Long id,
                                @Valid CreateSessionRequest req) {
        FreeDiveSession original = checkAccess(id);
        User me = currentUser();
        if (FreeDiveSession.countByOwner(me.id) >= MAX_SESSIONS) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity("Limite de " + MAX_SESSIONS + " sessions libres atteinte")
                            .build());
        }
        FreeDiveSession copy = new FreeDiveSession();
        copy.owner     = me;
        copy.label     = req.label() != null ? req.label() : original.label;
        copy.diveDate  = req.diveDate();
        copy.startTime = req.startTime();
        copy.notes     = original.notes;
        copy.persist();

        for (FreeSessionDiver orig : FreeSessionDiver.findBySession(original.id)) {
            FreeSessionDiver d = new FreeSessionDiver();
            d.session       = copy;
            d.firstName     = orig.firstName;
            d.lastName      = orig.lastName;
            d.level         = orig.level;
            d.email         = orig.email;
            d.phone         = orig.phone;
            d.isDirector    = orig.isDirector;
            d.aptitudes     = orig.aptitudes;
            d.licenseNumber = orig.licenseNumber;
            d.club          = orig.club;
            d.persist();
        }

        return Response.status(201).entity(SessionResponse.from(copy)).build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Plongeurs
    // ════════════════════════════════════════════════════════════════════════

    @GET
    @Path("/{id}/divers")
    public List<DiverResponse> listDivers(@PathParam("id") Long id) {
        checkAccess(id);
        return FreeSessionDiver.findBySession(id).stream()
                .sorted(Comparator.<FreeSessionDiver, Boolean>comparing(d -> !d.isDirector)
                        .thenComparing(d -> d.lastName))
                .map(DiverResponse::from)
                .toList();
    }

    @POST
    @Path("/{id}/divers")
    @Transactional
    public Response addDiver(@PathParam("id") Long id,
                             @Valid CreateDiverRequest req) {
        FreeDiveSession session = checkAccess(id);
        if (FreeSessionDiver.existsBySessionAndName(id, req.firstName(), req.lastName())) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity("Un plongeur avec ce nom et prénom est déjà dans cette session")
                            .build());
        }
        FreeSessionDiver d = new FreeSessionDiver();
        d.session      = session;
        d.firstName    = NameUtil.capitalize(req.firstName());
        d.lastName     = req.lastName() != null ? req.lastName().trim().toUpperCase() : null;
        d.level        = req.level();
        d.email        = req.email();
        d.phone        = req.phone();
        d.isDirector   = req.isDirector();
        d.aptitudes    = req.aptitudes();
        d.licenseNumber = req.licenseNumber();
        d.club         = req.club();
        d.persist();
        return Response.status(201).entity(DiverResponse.from(d)).build();
    }

    @PUT
    @Path("/{id}/divers/{did}")
    @Transactional
    public DiverResponse updateDiver(@PathParam("id") Long id,
                                     @PathParam("did") Long did,
                                     @Valid UpdateDiverRequest req) {
        checkAccess(id);
        FreeSessionDiver d = findDiver(id, did);
        if (FreeSessionDiver.existsBySessionAndNameExcluding(id, req.firstName(), req.lastName(), did)) {
            throw new BadRequestException(
                    "Un autre plongeur avec ce nom et prénom est déjà dans cette session");
        }
        d.firstName    = NameUtil.capitalize(req.firstName());
        d.lastName     = req.lastName() != null ? req.lastName().trim().toUpperCase() : null;
        d.level        = req.level();
        d.email        = req.email();
        d.phone        = req.phone();
        d.isDirector   = req.isDirector();
        d.aptitudes    = req.aptitudes();
        d.licenseNumber = req.licenseNumber();
        d.club         = req.club();
        return DiverResponse.from(d);
    }

    @DELETE
    @Path("/{id}/divers/{did}")
    @Transactional
    public Response removeDiver(@PathParam("id") Long id,
                                @PathParam("did") Long did) {
        checkAccess(id);
        FreeSessionDiver d = findDiver(id, did);
        // Supprime les appartenances aux palanquées (cascade)
        FreePalanqueeMember.deleteByDiver(d.id);
        d.delete();
        return Response.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Plongées (multi-plongées)
    // ════════════════════════════════════════════════════════════════════════

    @GET
    @Path("/{id}/dives")
    public List<DiveResponse> listDives(@PathParam("id") Long id) {
        checkAccess(id);
        return FreeSessionDive.findBySession(id).stream()
                .map(DiveResponse::from)
                .toList();
    }

    @POST
    @Path("/{id}/dives")
    @Transactional
    public Response createDive(@PathParam("id") Long id,
                               CreateDiveRequest req) {
        FreeDiveSession session = checkAccess(id);
        long count = FreeSessionDive.count("session.id", id);
        FreeSessionDive dive = new FreeSessionDive();
        dive.session   = session;
        dive.diveIndex = (int) count + 1;
        if (req != null) {
            dive.label     = req.label();
            dive.startTime = req.startTime();
            dive.endTime   = req.endTime();
            dive.depth     = req.depth();
            dive.duration  = req.duration();
        }
        dive.persist();
        return Response.status(201).entity(DiveResponse.from(dive)).build();
    }

    @PATCH
    @Path("/{id}/dives/{diveId}")
    @Transactional
    public DiveResponse updateDive(@PathParam("id") Long id,
                                   @PathParam("diveId") Long diveId,
                                   UpdateDiveRequest req) {
        checkAccess(id);
        FreeSessionDive dive = findDive(id, diveId);
        if (req != null) {
            dive.label     = req.label();
            dive.startTime = req.startTime();
            dive.endTime   = req.endTime();
            dive.depth     = req.depth();
            dive.duration  = req.duration();
        }
        return DiveResponse.from(dive);
    }

    @DELETE
    @Path("/{id}/dives/{diveId}")
    @Transactional
    public Response deleteDive(@PathParam("id") Long id,
                               @PathParam("diveId") Long diveId) {
        checkAccess(id);
        FreeSessionDive dive = findDive(id, diveId);

        // Récupérer les plongées restantes AVANT suppression
        List<FreeSessionDive> remaining = FreeSessionDive.findBySession(id)
                .stream()
                .filter(d -> !d.id.equals(diveId))
                .toList();
        boolean isLastDive = remaining.isEmpty();

        // Gérer les palanquées associées à cette plongée
        if (isLastDive) {
            // Dernière plongée : détacher les palanquées pour revenir en mode mono-plongée
            FreePalanquee.update("dive = null WHERE dive.id = ?1", diveId);
        } else {
            // Ce n'est pas la dernière : détacher les palanquées (conservées sans plongée assignée)
            FreePalanquee.update("dive = null WHERE dive.id = ?1", diveId);
        }

        dive.delete();

        // Réindexer les plongées restantes
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).diveIndex = i + 1;
        }

        // Si c'était la dernière plongée, dédupliquer les membres de palanquée :
        // un plongeur peut figurer dans plusieurs palanquées (multi-plongées) ; en mode
        // simple il ne doit apparaître qu'une seule fois (dans la première palanquée trouvée).
        if (isLastDive) {
            deduplicatePalanqueeMembers(id);
        }

        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/dives/assign")
    @Transactional
    public Response assignPalanqueeToDive(@PathParam("id") Long id,
                                          @Valid AssignPalanqueeToDiveRequest req) {
        checkAccess(id);
        FreePalanquee pal = FreePalanquee.findById(req.palanqueeId());
        if (pal == null || !pal.session.id.equals(id)) {
            throw new NotFoundException("Palanquée non trouvée dans cette session");
        }
        if (req.diveId() == null) {
            pal.dive = null;
        } else {
            FreeSessionDive dive = findDive(id, req.diveId());
            pal.dive = dive;
        }
        return Response.ok().build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Palanquées
    // ════════════════════════════════════════════════════════════════════════

    @GET
    @Path("/{id}/palanquees")
    public List<PalanqueeResponse> listPalanquees(@PathParam("id") Long id) {
        checkAccess(id);
        return FreePalanquee.findBySession(id).stream()
                .map(PalanqueeResponse::from)
                .toList();
    }

    @POST
    @Path("/{id}/palanquees")
    @Transactional
    public Response createPalanquee(@PathParam("id") Long id,
                                    @Valid CreatePalanqueeRequest req) {
        FreeDiveSession session = checkAccess(id);
        long count = FreePalanquee.count("session.id", id);
        FreePalanquee p = new FreePalanquee();
        p.session  = session;
        p.name     = req.name();
        p.position = (int) count;
        p.persist();
        return Response.status(201).entity(PalanqueeResponse.from(p)).build();
    }

    @PUT
    @Path("/{id}/palanquees/{pid}")
    @Transactional
    public PalanqueeResponse updatePalanquee(@PathParam("id") Long id,
                                             @PathParam("pid") Long pid,
                                             @Valid UpdatePalanqueeRequest req) {
        checkAccess(id);
        FreePalanquee p = findPalanquee(id, pid);
        p.name     = req.name();
        p.depth    = req.depth();
        p.duration = req.duration();
        return PalanqueeResponse.from(p);
    }

    @DELETE
    @Path("/{id}/palanquees/{pid}")
    @Transactional
    public Response deletePalanquee(@PathParam("id") Long id,
                                    @PathParam("pid") Long pid) {
        checkAccess(id);
        FreePalanquee p = findPalanquee(id, pid);
        // Les membres sont supprimés en cascade via ON DELETE CASCADE
        p.delete();
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/palanquees/assign")
    @Transactional
    public Response assignDiver(@PathParam("id") Long id,
                                @Valid AssignDiverRequest req) {
        checkAccess(id);
        FreeSessionDiver diver = FreeSessionDiver.findById(req.diverId());
        if (diver == null || !diver.session.id.equals(id)) {
            throw new NotFoundException("Plongeur non trouvé dans cette session");
        }

        if (req.palanqueeId() == null) {
            if (req.fromPalanqueeId() != null) {
                FreePalanqueeMember.deleteByDiverAndPalanquee(diver.id, req.fromPalanqueeId());
            } else {
                FreePalanqueeMember.deleteByDiver(diver.id);
            }
        } else {
            FreePalanquee target = findPalanquee(id, req.palanqueeId());
            if (req.fromPalanqueeId() != null && !req.fromPalanqueeId().equals(req.palanqueeId())) {
                FreePalanqueeMember.deleteByDiverAndPalanquee(diver.id, req.fromPalanqueeId());
            }
            if (FreePalanqueeMember.findByDiverAndPalanquee(diver.id, req.palanqueeId()) == null) {
                FreePalanqueeMember m = new FreePalanqueeMember();
                m.palanquee = target;
                m.diver     = diver;
                m.position  = (int) FreePalanqueeMember.count("palanquee.id = ?1", req.palanqueeId());
                m.persist();
            }
        }
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/palanquees/{pid}/reorder")
    @Transactional
    public Response reorderPalanquee(@PathParam("id") Long id,
                                     @PathParam("pid") Long pid,
                                     @Valid ReorderRequest req) {
        checkAccess(id);
        findPalanquee(id, pid);
        List<Long> ids = req.diverIds();
        for (int i = 0; i < ids.size(); i++) {
            FreePalanqueeMember m = FreePalanqueeMember.findByDiverAndPalanquee(ids.get(i), pid);
            if (m != null) m.position = i;
        }
        return Response.ok().build();
    }

    @PATCH
    @Path("/{id}/palanquees/{pid}/members/{did}/aptitudes")
    @Transactional
    public Response updateMemberAptitudes(@PathParam("id") Long id,
                                          @PathParam("pid") Long pid,
                                          @PathParam("did") Long did,
                                          UpdateMemberAptitudesRequest req) {
        checkAccess(id);
        FreePalanqueeMember member = FreePalanqueeMember.findByDiverAndPalanquee(did, pid);
        if (member == null) throw new NotFoundException("Membre non trouvé dans cette palanquée");
        member.aptitudes = (req != null && req.aptitudes() != null && !req.aptitudes().isBlank())
                ? req.aptitudes() : null;
        return Response.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    /** Vérifie que la session existe et que l'utilisateur courant en est propriétaire (ou ADMIN). */
    private FreeDiveSession checkAccess(Long sessionId) {
        FreeDiveSession s = FreeDiveSession.findById(sessionId);
        if (s == null) throw new NotFoundException("Session libre non trouvée");

        if (identity.hasRole("ADMIN")) return s;

        String principalName = identity.getPrincipal().getName();
        User me = User.findByEmail(principalName);
        if (me == null || s.owner == null || !s.owner.id.equals(me.id)) {
            throw new ForbiddenException("Accès réservé au propriétaire de la session");
        }
        return s;
    }

    private User currentUser() {
        String email = identity.getPrincipal().getName();
        User me = User.findByEmail(email);
        if (me == null) throw new NotFoundException("Utilisateur introuvable");
        return me;
    }

    private FreeSessionDiver findDiver(Long sessionId, Long diverId) {
        FreeSessionDiver d = FreeSessionDiver.findById(diverId);
        if (d == null || !d.session.id.equals(sessionId)) {
            throw new NotFoundException("Plongeur non trouvé dans cette session");
        }
        return d;
    }

    private FreeSessionDive findDive(Long sessionId, Long diveId) {
        FreeSessionDive d = FreeSessionDive.findById(diveId);
        if (d == null || !d.session.id.equals(sessionId)) {
            throw new NotFoundException("Plongée non trouvée dans cette session");
        }
        return d;
    }

    private FreePalanquee findPalanquee(Long sessionId, Long palanqueeId) {
        FreePalanquee p = FreePalanquee.findById(palanqueeId);
        if (p == null || !p.session.id.equals(sessionId)) {
            throw new NotFoundException("Palanquée non trouvée dans cette session");
        }
        return p;
    }

    /**
     * Déduplication des FreePalanqueeMember après suppression de la dernière plongée.
     * En mode multi-plongées, un plongeur peut figurer dans plusieurs palanquées.
     * Quand on revient en mode simple (0 plongée), on ne garde que la première
     * appartenance (ordre palanquée par position puis id) et on supprime les doublons.
     */
    private void deduplicatePalanqueeMembers(Long sessionId) {
        List<FreePalanquee> palList = FreePalanquee.list("session.id = ?1 ORDER BY position, id", sessionId);
        java.util.Set<Long> seenDiverIds = new java.util.HashSet<>();
        for (FreePalanquee pal : palList) {
            List<FreePalanqueeMember> members = FreePalanqueeMember.findByPalanquee(pal.id);
            for (FreePalanqueeMember m : members) {
                if (!seenDiverIds.add(m.diver.id)) {
                    // Ce plongeur est déjà dans une palanquée précédente → doublon
                    m.delete();
                }
            }
        }
    }
}
