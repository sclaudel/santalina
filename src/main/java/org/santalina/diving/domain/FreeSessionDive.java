package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.List;

/**
 * Plongée d'une session libre (miroir de {@link SlotDive} pour les sessions libres).
 */
@Entity
@Table(name = "free_session_dives")
public class FreeSessionDive extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    public FreeDiveSession session;

    @Column(name = "dive_index", nullable = false)
    public int diveIndex = 1;

    @Column
    public String label;

    @Column(name = "start_time")
    public LocalTime startTime;

    @Column(name = "end_time")
    public LocalTime endTime;

    @Column
    public String depth;

    @Column
    public String duration;

    public static List<FreeSessionDive> findBySession(Long sessionId) {
        return list("session.id = ?1 ORDER BY diveIndex, id", sessionId);
    }
}
