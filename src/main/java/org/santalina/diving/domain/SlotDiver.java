package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
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

    @Column
    public String aptitudes;

    @Column(name = "license_number")
    public String licenseNumber;

    @Column(name = "medical_cert_date")
    public LocalDate medicalCertDate;

    @Column(columnDefinition = "TEXT")
    public String comment;

    @Column
    public String club;

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

    /** Vérifie si un email est le directeur de plongée assigné sur un créneau. */
    public static boolean isAssignedDirectorByEmail(Long slotId, String email) {
        if (email == null || email.isBlank()) return false;
        return count("slot.id = ?1 and isDirector = true and lower(email) = lower(?2)", slotId, email) > 0;
    }

    /** Retourne l'entrée d'un email pour un créneau donné (non-directeur). */
    public static SlotDiver findBySlotAndEmail(Long slotId, String email) {
        if (email == null || email.isBlank()) return null;
        return find("slot.id = ?1 and lower(email) = lower(?2)", slotId, email).firstResult();
    }
}
