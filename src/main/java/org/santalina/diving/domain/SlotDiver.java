package org.santalina.diving.domain;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palanquee_id")
    public Palanquee palanquee;

    @Column(name = "palanquee_position", nullable = false)
    public int palanqueePosition = 0;

    public static List<SlotDiver> findBySlot(Long slotId) {
        return list("slot.id", slotId);
    }

    public static long countBySlot(Long slotId) {
        return count("slot.id", slotId);
    }

    public static boolean hasDirector(Long slotId) {
        return count("slot.id = ?1 and isDirector = true", slotId) > 0;
    }

    public static List<SlotDiver> findBySlotIds(List<Long> slotIds) {
        if (slotIds.isEmpty()) return List.of();
        return list("slot.id in ?1", slotIds);
    }

    public static boolean existsBySlotAndName(Long slotId, String firstName, String lastName) {
        return count("slot.id = ?1 and lower(firstName) = lower(?2) and lower(lastName) = lower(?3)",
                slotId, firstName, lastName) > 0;
    }

    public static List<SlotDiver> findByPalanquee(Long palanqueeId) {
        return list("palanquee.id = ?1 ORDER BY palanqueePosition, id", palanqueeId);
    }

    public static boolean existsBySlotAndNameExcluding(Long slotId, String firstName, String lastName, Long excludeId) {
        return count("slot.id = ?1 and lower(firstName) = lower(?2) and lower(lastName) = lower(?3) and id != ?4",
                slotId, firstName, lastName, excludeId) > 0;
    }
}
