package com.example.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "slot_divers")
public class SlotDiver extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    public DiveSlot slot;

    @Column(name = "first_name", nullable = false)
    public String firstName;

    @Column(name = "last_name", nullable = false)
    public String lastName;

    @Column(nullable = false)
    public String level;

    @Column
    public String email;

    @Column
    public String phone;

    @Column(name = "is_director", nullable = false)
    public boolean isDirector = false;

    @Column(name = "added_at", nullable = false)
    public LocalDateTime addedAt = LocalDateTime.now();

    public static List<SlotDiver> findBySlot(Long slotId) {
        return list("slot.id", slotId);
    }

    public static long countBySlot(Long slotId) {
        return count("slot.id", slotId);
    }

    public static boolean hasDirector(Long slotId) {
        return count("slot.id = ?1 and isDirector = true", slotId) > 0;
    }
}

