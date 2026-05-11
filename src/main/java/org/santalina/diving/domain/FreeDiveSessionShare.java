package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Partage d'une session libre entre directeurs de plongée.
 * Le propriétaire de la session peut partager celle-ci avec d'autres DP
 * en lecture seule ("READ") ou en écriture ("WRITE").
 * Les sessions partagées ne comptent pas dans le quota du destinataire.
 */
@Entity
@Table(name = "free_session_shares")
public class FreeDiveSessionShare extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    public FreeDiveSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    public User sharedWith;

    /** Niveau d'accès : "READ" ou "WRITE". */
    @Column(name = "access_level", nullable = false)
    public String accessLevel = "READ";

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public static List<FreeDiveSessionShare> findBySession(Long sessionId) {
        return list("session.id = ?1 ORDER BY createdAt", sessionId);
    }

    public static List<FreeDiveSessionShare> findBySharedWith(Long userId) {
        return list("sharedWith.id = ?1 ORDER BY session.diveDate DESC, session.startTime DESC, session.id DESC", userId);
    }

    public static FreeDiveSessionShare findBySessionAndUser(Long sessionId, Long userId) {
        return find("session.id = ?1 AND sharedWith.id = ?2", sessionId, userId).firstResult();
    }
}
