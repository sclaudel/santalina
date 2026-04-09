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

    @Column(name = "added_at", nullable = false)
    public LocalDateTime addedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palanquee_id")
    public Palanquee palanquee;

    @Column(name = "palanquee_position", nullable = false)
    public int palanqueePosition = 0;

    // ---- Champs inscription en file d'attente ----

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false)
    public RegistrationStatus registrationStatus = RegistrationStatus.CONFIRMED;

    @Column(name = "registered_user_id")
    public Long registeredUserId;

    @Column(name = "number_of_dives")
    public Integer numberOfDives;

    @Column(name = "last_dive_date")
    public LocalDate lastDiveDate;

    @Column(name = "prepared_level")
    public String preparedLevel;

    @Column(name = "registration_comment", columnDefinition = "TEXT")
    public String registrationComment;

    @Column(name = "registration_validated_at")
    public LocalDateTime registrationValidatedAt;

    // ---- Panache finders ----

    public static List<SlotDiver> findBySlot(Long slotId) {
        return list("slot.id", slotId);
    }

    /** Retourne uniquement les plongeurs CONFIRMÉS (exclut la file d'attente) */
    public static List<SlotDiver> findConfirmedBySlot(Long slotId) {
        return list("slot.id = ?1 AND registrationStatus = ?2 ORDER BY addedAt",
                slotId, RegistrationStatus.CONFIRMED);
    }

    /** Retourne la file d'attente (PENDING), triée par date d'inscription */
    public static List<SlotDiver> findPendingBySlot(Long slotId) {
        return list("slot.id = ?1 AND registrationStatus = ?2 ORDER BY addedAt",
                slotId, RegistrationStatus.PENDING);
    }

    public static long countBySlot(Long slotId) {
        return count("slot.id", slotId);
    }

    public static long countConfirmedBySlot(Long slotId) {
        return count("slot.id = ?1 AND registrationStatus = ?2", slotId, RegistrationStatus.CONFIRMED);
    }

    public static boolean hasDirector(Long slotId) {
        return count("slot.id = ?1 and isDirector = true", slotId) > 0;
    }

    /** Retourne l'entrée directeur de plongée d'un créneau */
    public static SlotDiver findDirector(Long slotId) {
        return find("slot.id = ?1 AND isDirector = true", slotId).firstResult();
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

    /** Trouve l'inscription d'un utilisateur donné sur un créneau */
    public static SlotDiver findBySlotAndRegisteredUser(Long slotId, Long userId) {
        return find("slot.id = ?1 AND registeredUserId = ?2", slotId, userId).firstResult();
    }
}
