package org.santalina.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Plongeur d'une session libre (miroir de {@link SlotDiver} sans restriction de capacité).
 */
@Entity
@Table(name = "free_session_divers")
public class FreeSessionDiver extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    public FreeDiveSession session;

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

    public static List<FreeSessionDiver> findBySession(Long sessionId) {
        return list("session.id = ?1", sessionId);
    }

    public static boolean existsBySessionAndName(Long sessionId, String firstName, String lastName) {
        return count("session.id = ?1 and lower(firstName) = lower(?2) and lower(lastName) = lower(?3)",
                sessionId, firstName, lastName) > 0;
    }

    public static boolean existsBySessionAndNameExcluding(Long sessionId, String firstName, String lastName, Long excludeId) {
        return count("session.id = ?1 and lower(firstName) = lower(?2) and lower(lastName) = lower(?3) and id != ?4",
                sessionId, firstName, lastName, excludeId) > 0;
    }
}
