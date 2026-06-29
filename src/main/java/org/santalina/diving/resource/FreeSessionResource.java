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
 * Les sessions partagées avec un DP ne comptent pas dans son quota.
 * <p>
 * Endpoints :
 * <ul>
 *   <li>GET    /api/free-sessions                                             — liste ses sessions (propriétaire)</li>
 *   <li>GET    /api/free-sessions/{id}                                        — détail d'une session avec niveau d'accès</li>
 *   <li>GET    /api/free-sessions/shared                                      — sessions partagées avec moi</li>
 *   <li>POST   /api/free-sessions                                             — créer</li>
 *   <li>PUT    /api/free-sessions/{id}                                        — modifier (propriétaire)</li>
 *   <li>DELETE /api/free-sessions/{id}                                        — supprimer (propriétaire)</li>
 *   <li>GET    /api/free-sessions/{id}/shares                                 — liste des partages (propriétaire)</li>
 *   <li>POST   /api/free-sessions/{id}/shares                                 — partager (propriétaire)</li>
 *   <li>PUT    /api/free-sessions/{id}/shares/{shareId}                       — modifier niveau accès (propriétaire)</li>
 *   <li>DELETE /api/free-sessions/{id}/shares/{shareId}                       — révoquer partage (propriétaire)</li>
 *   <li>DELETE /api/free-sessions/{id}/shares/me                              — quitter une session partagée</li>
 *   <li>GET    /api/free-sessions/{id}/search-dp                              — rechercher un DP pour partager</li>
 *   <li>GET    /api/free-sessions/{id}/divers                                 — liste plongeurs</li>
 *   <li>POST   /api/free-sessions/{id}/divers                                 — ajouter (accès WRITE)</li>
 *   <li>PUT    /api/free-sessions/{id}/divers/{did}                           — modifier (accès WRITE)</li>
 *   <li>DELETE /api/free-sessions/{id}/divers/{did}                           — supprimer (accès WRITE)</li>
 *   <li>GET    /api/free-sessions/{id}/dives                                  — liste plongées</li>
 *   <li>POST   /api/free-sessions/{id}/dives                                  — créer plongée (accès WRITE)</li>
 *   <li>PATCH  /api/free-sessions/{id}/dives/{diveId}                         — modifier (accès WRITE)</li>
 *   <li>DELETE /api/free-sessions/{id}/dives/{diveId}                         — supprimer (accès WRITE)</li>
 *   <li>PUT    /api/free-sessions/{id}/dives/assign                           — assigner palanquée ↔ plongée (accès WRITE)</li>
 *   <li>GET    /api/free-sessions/{id}/palanquees                             — liste palanquées</li>
 *   <li>POST   /api/free-sessions/{id}/palanquees                             — créer (accès WRITE)</li>
 *   <li>PUT    /api/free-sessions/{id}/palanquees/{pid}                       — renommer (accès WRITE)</li>
 *   <li>DELETE /api/free-sessions/{id}/palanquees/{pid}                       — supprimer (accès WRITE)</li>
 *   <li>PUT    /api/free-sessions/{id}/palanquees/assign                      — assigner plongeur (accès WRITE)</li>
 *   <li>PUT    /api/free-sessions/{id}/palanquees/{pid}/reorder               — réordonner (accès WRITE)</li>
 *   <li>PATCH  /api/free-sessions/{id}/palanquees/{pid}/members/{did}/aptitudes — aptitudes (accès WRITE)</li>
 * </ul>
 */
@Path("/api/free-sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
@Tag(name = "Sessions libres")
public class FreeSessionResource {

    /** Nombre maximum de sessions libres par DP (sessions dont le DP est propriétaire). */
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

    @GET
    @Path("/shared")
    public List<SessionResponse> listSharedSessions() {
        User me = currentUser();
        return FreeDiveSessionShare.findBySharedWith(me.id).stream()
                .map(share -> SessionResponse.fromShared(share.session, share))
                .toList();
    }

    @GET
    @Path("/{id}")
    public SessionResponse getSession(@PathParam("id") Long id) {
        Access a = checkAccess(id);
        return a.isOwner()
                ? SessionResponse.from(a.session())
                : SessionResponse.fromShared(a.session(),
                        FreeDiveSessionShare.findBySessionAndUser(id, currentUser().id));
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
        FreeDiveSession s = requireOwner(id);
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
        FreeDiveSession s = requireOwner(id);
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
        FreeDiveSession original = requireRead(id);
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
        requireRead(id);
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
        FreeDiveSession session = requireWrite(id);
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
        requireWrite(id);
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
        d.fonction     = req.fonction();
        d.licenseNumber = req.licenseNumber();
        d.club         = req.club();
        return DiverResponse.from(d);
    }

    @DELETE
    @Path("/{id}/divers/{did}")
    @Transactional
    public Response removeDiver(@PathParam("id") Long id,
                                @PathParam("did") Long did) {
        requireWrite(id);
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
        requireRead(id);
        return FreeSessionDive.findBySession(id).stream()
                .map(DiveResponse::from)
                .toList();
    }

    @POST
    @Path("/{id}/dives")
    @Transactional
    public Response createDive(@PathParam("id") Long id,
                               CreateDiveRequest req) {
        FreeDiveSession session = requireWrite(id);
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
        requireWrite(id);
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
        requireWrite(id);
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
        requireWrite(id);
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
        requireRead(id);
        return FreePalanquee.findBySession(id).stream()
                .map(PalanqueeResponse::from)
                .toList();
    }

    @POST
    @Path("/{id}/palanquees")
    @Transactional
    public Response createPalanquee(@PathParam("id") Long id,
                                    @Valid CreatePalanqueeRequest req) {
        FreeDiveSession session = requireWrite(id);
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
        requireWrite(id);
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
        requireWrite(id);
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
        requireWrite(id);
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
            // Conserver les aptitudes et fonction de l'ancienne palanquée
            String previousAptitudes = null;
            String previousFonction = null;
            if (req.fromPalanqueeId() != null && !req.fromPalanqueeId().equals(req.palanqueeId())) {
                FreePalanqueeMember existing = FreePalanqueeMember.findByDiverAndPalanquee(diver.id, req.fromPalanqueeId());
                if (existing != null) {
                    previousAptitudes = existing.aptitudes;
                    previousFonction = existing.fonction;
                }
                FreePalanqueeMember.deleteByDiverAndPalanquee(diver.id, req.fromPalanqueeId());
            }
            if (FreePalanqueeMember.findByDiverAndPalanquee(diver.id, req.palanqueeId()) == null) {
                FreePalanqueeMember m = new FreePalanqueeMember();
                m.palanquee = target;
                m.diver     = diver;
                m.position  = (int) FreePalanqueeMember.count("palanquee.id = ?1", req.palanqueeId());
                m.aptitudes = previousAptitudes;
                m.fonction  = previousFonction;
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
        requireWrite(id);
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
        requireWrite(id);
        FreePalanqueeMember member = FreePalanqueeMember.findByDiverAndPalanquee(did, pid);
        if (member == null) throw new NotFoundException("Membre non trouvé dans cette palanquée");
        member.aptitudes = (req != null && req.aptitudes() != null && !req.aptitudes().isBlank())
                ? req.aptitudes() : null;
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}/palanquees/{pid}/members/{did}/fonction")
    @Transactional
    public Response updateMemberFonction(@PathParam("id") Long id,
                                         @PathParam("pid") Long pid,
                                         @PathParam("did") Long did,
                                         UpdateMemberFonctionRequest req) {
        requireWrite(id);
        FreePalanqueeMember member = FreePalanqueeMember.findByDiverAndPalanquee(did, pid);
        if (member == null) throw new NotFoundException("Membre non trouvé dans cette palanquée");
        member.fonction = (req != null && req.fonction() != null && !req.fonction().isBlank())
                ? req.fonction() : null;
        return Response.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Partage
    // ════════════════════════════════════════════════════════════════════════

    /** Liste les partages d'une session (propriétaire uniquement). */
    @GET
    @Path("/{id}/shares")
    public List<ShareResponse> listShares(@PathParam("id") Long id) {
        requireOwner(id);
        return FreeDiveSessionShare.findBySession(id).stream()
                .map(ShareResponse::from)
                .toList();
    }

    /** Partage une session avec un autre DP (propriétaire uniquement). */
    @POST
    @Path("/{id}/shares")
    @Transactional
    public Response addShare(@PathParam("id") Long id,
                             @Valid ShareRequest req) {
        FreeDiveSession session = requireOwner(id);
        User me = currentUser();
        if (req.sharedWithUserId().equals(me.id)) {
            throw new BadRequestException("Vous ne pouvez pas partager une session avec vous-même");
        }
        User target = User.findById(req.sharedWithUserId());
        if (target == null) throw new NotFoundException("Utilisateur destinataire non trouvé");
        if (!target.roles.contains(UserRole.DIVE_DIRECTOR) && !target.roles.contains(UserRole.ADMIN)) {
            throw new BadRequestException("Le partage est réservé aux directeurs de plongée");
        }
        if (FreeDiveSessionShare.findBySessionAndUser(session.id, target.id) != null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity("Cette session est déjà partagée avec cet utilisateur")
                            .build());
        }
        String level = "READ".equals(req.accessLevel()) || "WRITE".equals(req.accessLevel())
                ? req.accessLevel() : "READ";
        FreeDiveSessionShare share = new FreeDiveSessionShare();
        share.session    = session;
        share.sharedWith = target;
        share.accessLevel = level;
        share.persist();
        return Response.status(201).entity(ShareResponse.from(share)).build();
    }

    /** Modifie le niveau d'accès d'un partage (propriétaire uniquement). */
    @PUT
    @Path("/{id}/shares/{shareId}")
    @Transactional
    public ShareResponse updateShare(@PathParam("id") Long id,
                                     @PathParam("shareId") Long shareId,
                                     @Valid UpdateShareRequest req) {
        requireOwner(id);
        FreeDiveSessionShare share = FreeDiveSessionShare.findById(shareId);
        if (share == null || !share.session.id.equals(id)) throw new NotFoundException("Partage non trouvé");
        String level = "READ".equals(req.accessLevel()) || "WRITE".equals(req.accessLevel())
                ? req.accessLevel() : "READ";
        share.accessLevel = level;
        return ShareResponse.from(share);
    }

    /** Révoque un partage (propriétaire uniquement). */
    @DELETE
    @Path("/{id}/shares/{shareId}")
    @Transactional
    public Response deleteShare(@PathParam("id") Long id,
                                @PathParam("shareId") Long shareId) {
        requireOwner(id);
        FreeDiveSessionShare share = FreeDiveSessionShare.findById(shareId);
        if (share == null || !share.session.id.equals(id)) throw new NotFoundException("Partage non trouvé");
        share.delete();
        return Response.noContent().build();
    }

    /** Quitter une session partagée (destinataire uniquement). */
    @DELETE
    @Path("/{id}/shares/me")
    @Transactional
    public Response leaveShare(@PathParam("id") Long id) {
        User me = currentUser();
        FreeDiveSessionShare share = FreeDiveSessionShare.findBySessionAndUser(id, me.id);
        if (share == null) throw new NotFoundException("Vous n'avez pas accès à cette session partagée");
        share.delete();
        return Response.noContent().build();
    }

    /** Recherche de directeurs de plongée pour le partage (uniquement si propriétaire). */
    @GET
    @Path("/{id}/search-dp")
    public List<DpSearchResult> searchDpForShare(@PathParam("id") Long id,
                                                  @QueryParam("q") String q) {
        requireOwner(id);
        User me = currentUser();
        if (q == null || q.isBlank()) return List.of();
        String pattern = "%" + q.trim().toLowerCase() + "%";
        // Chercher les DP/ADMIN, exclure le propriétaire lui-même
        return User.<User>list(
                        "(LOWER(firstName) LIKE ?1 OR LOWER(lastName) LIKE ?1 OR LOWER(email) LIKE ?1"
                        + " OR LOWER(CONCAT(firstName, ' ', lastName)) LIKE ?1)"
                        + " AND id != ?2", pattern, me.id)
                .stream()
                .filter(u -> u.roles.contains(UserRole.DIVE_DIRECTOR) || u.roles.contains(UserRole.ADMIN))
                .limit(10)
                .map(DpSearchResult::from)
                .toList();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    private record Access(FreeDiveSession session, String level) {
        boolean isOwner()  { return "OWNER".equals(level); }
        boolean canWrite() { return "OWNER".equals(level) || "WRITE".equals(level); }
    }

    /**
     * Vérifie qu'on a au moins un accès en lecture (propriétaire, partagé READ ou WRITE, ou ADMIN).
     * Retourne un objet Access avec le niveau réel.
     */
    private Access checkAccess(Long sessionId) {
        FreeDiveSession s = FreeDiveSession.findById(sessionId);
        if (s == null) throw new NotFoundException("Session libre non trouvée");

        if (identity.hasRole("ADMIN")) return new Access(s, "OWNER");

        User me = currentUser();
        if (s.owner != null && s.owner.id.equals(me.id)) return new Access(s, "OWNER");

        FreeDiveSessionShare share = FreeDiveSessionShare.findBySessionAndUser(sessionId, me.id);
        if (share != null) return new Access(s, share.accessLevel);

        throw new ForbiddenException("Accès refusé à cette session");
    }

    /** Accès en lecture seule (ou plus). */
    private FreeDiveSession requireRead(Long id) {
        return checkAccess(id).session();
    }

    /** Accès en écriture (WRITE ou propriétaire). */
    private FreeDiveSession requireWrite(Long id) {
        Access a = checkAccess(id);
        if (!a.canWrite()) throw new ForbiddenException("Accès en écriture requis");
        return a.session();
    }

    /** Accès propriétaire uniquement. Retourne le User courant pour éviter une double lookup. */
    private FreeDiveSession requireOwner(Long id) {
        Access a = checkAccess(id);
        if (!a.isOwner()) throw new ForbiddenException("Réservé au propriétaire de la session");
        return a.session();
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
