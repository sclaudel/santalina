package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.List;

/**
 * Appartenance d'un plongeur à une palanquée.
 * Remplace le FK palanquee_id sur SlotDiver afin de permettre
 * à un plongeur de participer à plusieurs plongées (multi-plongées par créneau).
 */
@Entity
@Table(name = "palanquee_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"palanquee_id", "slot_diver_id"}))
public class PalanqueeMember extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palanquee_id", nullable = false)
    public Palanquee palanquee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_diver_id", nullable = false)
    public SlotDiver diver;

    @Column(nullable = false)
    public int position = 0;

    /** Aptitudes spécifiques à cette plongée. Si null, utiliser SlotDiver.aptitudes. */
    @Column
    public String aptitudes;

    /** Membres d'une palanquée, ordonnés par position puis id. */
    public static List<PalanqueeMember> findByPalanquee(Long palanqueeId) {
        return list("palanquee.id = ?1 ORDER BY position, id", palanqueeId);
    }

    /** Cherche l'appartenance d'un plongeur dans une palanquée donnée. */
    public static PalanqueeMember findByDiverAndPalanquee(Long diverId, Long palanqueeId) {
        return find("diver.id = ?1 AND palanquee.id = ?2", diverId, palanqueeId).firstResult();
    }

    /** Supprime toutes les appartenances d'un plongeur (désassignation complète). */
    public static void deleteByDiver(Long diverId) {
        delete("diver.id = ?1", diverId);
    }

    /** Supprime l'appartenance d'un plongeur dans une palanquée spécifique. */
    public static void deleteByDiverAndPalanquee(Long diverId, Long palanqueeId) {
        delete("diver.id = ?1 AND palanquee.id = ?2", diverId, palanqueeId);
    }
}
