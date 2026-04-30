package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fiche de sécurité (image ou PDF) déposée par le DP sur un créneau passé.
 * Les fichiers sont automatiquement supprimés 1 an après leur dépôt.
 */
@Entity
@Table(name = "slot_safety_sheets")
public class SlotSafetySheet extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    public DiveSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    public User uploader;

    /** Nom d'origine du fichier tel que fourni par l'utilisateur. */
    @Column(name = "original_name", nullable = false)
    public String originalName;

    /** Nom stocké sur le disque (UUID + extension). */
    @Column(name = "stored_name", nullable = false)
    public String storedName;

    /** Chemin relatif au répertoire data. */
    @Column(name = "file_path", nullable = false)
    public String filePath;

    @Column(name = "content_type", nullable = false)
    public String contentType;

    @Column(name = "file_size", nullable = false)
    public long fileSize;

    @Column(name = "uploaded_at", nullable = false)
    public LocalDateTime uploadedAt = LocalDateTime.now();

    /** Date d'expiration = uploadedAt + 1 an. */
    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    // ── Finders ──────────────────────────────────────────────────────────────

    public static List<SlotSafetySheet> findBySlot(Long slotId) {
        return list("slot.id = ?1 ORDER BY uploadedAt ASC", slotId);
    }

    public static long countBySlot(Long slotId) {
        return count("slot.id = ?1", slotId);
    }

    public static List<SlotSafetySheet> findExpired(LocalDateTime cutoff) {
        return list("expiresAt <= ?1", cutoff);
    }
}
