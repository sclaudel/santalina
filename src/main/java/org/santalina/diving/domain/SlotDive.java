package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "slot_dives")
public class SlotDive extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    public DiveSlot slot;

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

    public static List<SlotDive> findBySlot(Long slotId) {
        return list("slot.id = ?1 ORDER BY diveIndex, id", slotId);
    }
}
