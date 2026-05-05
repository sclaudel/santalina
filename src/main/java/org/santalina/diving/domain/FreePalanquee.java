package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;

/**
 * Palanquée d'une session libre (miroir de {@link Palanquee}).
 */
@Entity
@Table(name = "free_palanquees")
public class FreePalanquee extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    public FreeDiveSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dive_id")
    public FreeSessionDive dive;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public int position = 0;

    @Column
    public String depth;

    @Column
    public String duration;

    public static List<FreePalanquee> findBySession(Long sessionId) {
        return list("session.id = ?1 ORDER BY position, id", sessionId);
    }
}
