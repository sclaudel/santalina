package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "palanquees")
public class Palanquee extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    public DiveSlot slot;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public int position = 0;

    public static List<Palanquee> findBySlot(Long slotId) {
        return list("slot.id = ?1 ORDER BY position, id", slotId);
    }
}
