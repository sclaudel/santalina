package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "dive_slots")
public class DiveSlot extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "slot_date", nullable = false)
    public LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    public LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    public LocalTime endTime;

    @Column(name = "diver_count", nullable = false)
    public int diverCount;

    @Column
    public String title;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "slot_type")
    public String slotType;

    @Column
    public String club;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = true)
    public User createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();

    // ---- Panache finders ----

    public static List<DiveSlot> findByDate(LocalDate date) {
        return list("slotDate", date);
    }

    /**
     * Retourne tous les créneaux qui se chevauchent avec la plage [start, end) à une date donnée.
     */
    public static List<DiveSlot> findOverlapping(LocalDate date, LocalTime start, LocalTime end, Long excludeId) {
        if (excludeId != null) {
            return list("slotDate = ?1 AND startTime < ?2 AND endTime > ?3 AND id <> ?4",
                    date, end, start, excludeId);
        }
        return list("slotDate = ?1 AND startTime < ?2 AND endTime > ?3",
                date, end, start);
    }

    public static List<DiveSlot> findByDateRange(LocalDate from, LocalDate to) {
        return list("slotDate >= ?1 AND slotDate <= ?2 ORDER BY slotDate, startTime", from, to);
    }

    public static List<DiveSlot> findByCreatorAndDateRange(Long creatorId, LocalDate from, LocalDate to) {
        return list("createdBy.id = ?1 AND slotDate >= ?2 AND slotDate <= ?3 ORDER BY slotDate, startTime",
                creatorId, from, to);
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
