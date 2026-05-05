package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;

/**
 * Appartenance d'un plongeur à une palanquée de session libre (miroir de {@link PalanqueeMember}).
 */
@Entity
@Table(name = "free_palanquee_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"palanquee_id", "diver_id"}))
public class FreePalanqueeMember extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palanquee_id", nullable = false)
    public FreePalanquee palanquee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diver_id", nullable = false)
    public FreeSessionDiver diver;

    @Column(nullable = false)
    public int position = 0;

    @Column
    public String aptitudes;

    public static List<FreePalanqueeMember> findByPalanquee(Long palanqueeId) {
        return list("palanquee.id = ?1 ORDER BY position, id", palanqueeId);
    }

    public static FreePalanqueeMember findByDiverAndPalanquee(Long diverId, Long palanqueeId) {
        return find("diver.id = ?1 AND palanquee.id = ?2", diverId, palanqueeId).firstResult();
    }

    public static void deleteByDiver(Long diverId) {
        delete("diver.id = ?1", diverId);
    }

    public static void deleteByDiverAndPalanquee(Long diverId, Long palanqueeId) {
        delete("diver.id = ?1 AND palanquee.id = ?2", diverId, palanqueeId);
    }
}
