package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entrée dans la liste d'attente pour l'auto-inscription sur un créneau.
 * La liste est triée par {@code registeredAt} (ordre d'arrivée).
 */
@Entity
@Table(name = "waiting_list_entries")
public class WaitingListEntry extends PanacheEntityBase {

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
    public String email;

    /** Niveau de certification du plongeur. */
    @Column(nullable = false)
    public String level;

    /** Nombre de plongées effectuées (facultatif). */
    @Column(name = "number_of_dives")
    public Integer numberOfDives;

    /** Date de la dernière plongée (facultatif). */
    @Column(name = "last_dive_date")
    public LocalDate lastDiveDate;

    /** Niveau en cours de préparation (facultatif). */
    @Column(name = "prepared_level")
    public String preparedLevel;

    /** Commentaire libre à destination du DP (optionnel). */
    @Column(columnDefinition = "TEXT")
    public String comment;

    /** Date de début de validité du certificat médical (obligatoire). */
    @Column(name = "medical_cert_date")
    public LocalDate medicalCertDate;

    /** Club d'appartenance du plongeur (optionnel). */
    @Column
    public String club;

    /** Atteste que le plongeur a vérifié la validité de sa licence FFESSM. */
    @Column(name = "license_confirmed", nullable = false)
    public boolean licenseConfirmed = false;

    @Column(name = "registered_at", nullable = false)
    public LocalDateTime registeredAt = LocalDateTime.now();

    // ---- Panache finders ----

    /** Retourne toutes les entrées d'un créneau, triées par date d'inscription. */
    public static List<WaitingListEntry> findBySlotOrdered(Long slotId) {
        return list("slot.id = ?1 ORDER BY registeredAt ASC", slotId);
    }

    /** Vérifie si un email est déjà inscrit sur un créneau. */
    public static boolean existsBySlotAndEmail(Long slotId, String email) {
        return count("slot.id = ?1 and lower(email) = lower(?2)", slotId, email) > 0;
    }

    /** Retourne l'entrée d'un email pour un créneau donné. */
    public static WaitingListEntry findBySlotAndEmail(Long slotId, String email) {
        return find("slot.id = ?1 and lower(email) = lower(?2)", slotId, email).firstResult();
    }

    /** Retourne le nombre d'entrées en attente pour un créneau. */
    public static long countBySlot(Long slotId) {
        return count("slot.id = ?1", slotId);
    }
}
