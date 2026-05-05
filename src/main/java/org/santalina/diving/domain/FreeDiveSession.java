package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Session de plongée libre : organisation DP sans créneau calendaire.
 * Chaque DP peut en posséder au plus 15 (règle appliquée dans la ressource REST).
 */
@Entity
@Table(name = "free_dive_sessions")
public class FreeDiveSession extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    public User owner;

    @Column
    public String label;

    @Column(name = "dive_date", nullable = false)
    public LocalDate diveDate;

    @Column(name = "start_time", nullable = false)
    public LocalTime startTime;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();

    public static List<FreeDiveSession> findByOwner(Long ownerId) {
        return list("owner.id = ?1 ORDER BY diveDate DESC, startTime DESC, id DESC", ownerId);
    }

    public static long countByOwner(Long ownerId) {
        return count("owner.id = ?1", ownerId);
    }
}
