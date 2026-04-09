-- ============================================================
--  V24__test_data.sql  –  Fixtures pour les tests d'intégration
--  CE FICHIER N'EST CHARGÉ QU'EN ENVIRONNEMENT DE TEST (H2)
--  grâce à quarkus.flyway.locations=db/migration,db/testdata
-- ============================================================

-- ── Utilisateurs de test (IDs hauts pour éviter les collisions) ──────────
INSERT INTO users (id, email, password_hash, name, first_name, last_name, role, activated, consent_given)
VALUES
  (1001, 'admin@santalina.com',  'test-hash', 'Admin Test',       'Admin',     'SANTALINA', 'ADMIN',          TRUE, TRUE),
  (1002, 'dp@test.com',          'test-hash', 'Directeur Test',   'Directeur', 'PLONGEE',   'DIVE_DIRECTOR',  TRUE, TRUE),
  (1003, 'dp2@test.com',         'test-hash', 'Directeur2 Test',  'Directeur', 'DEUX',      'DIVE_DIRECTOR',  TRUE, TRUE),
  (1004, 'diver@test.com',       'test-hash', 'Plongeur Test',    'Plongeur',  'TEST',      'DIVER',           TRUE, TRUE),
  (1005, 'diver2@test.com',      'test-hash', 'Plongeur2 Test',   'Plongeur',  'DEUX',      'DIVER',           TRUE, TRUE);

-- ── Créneaux de test ──────────────────────────────────────────────────────

-- 1001 : Inscriptions DÉSACTIVÉES
INSERT INTO dive_slots (id, slot_date, start_time, end_time, diver_count, registration_enabled)
VALUES (1001, '2099-12-01', '09:00', '12:00', 5, FALSE);

-- 1002 : Inscriptions activées SANS directeur affecté
INSERT INTO dive_slots (id, slot_date, start_time, end_time, diver_count, registration_enabled)
VALUES (1002, '2099-12-02', '09:00', '12:00', 5, TRUE);

-- 1003 : Inscriptions activées, dp@test.com directeur, 5 places libres
--        → utilisé pour tester l'inscription réussie
INSERT INTO dive_slots (id, slot_date, start_time, end_time, diver_count, registration_enabled)
VALUES (1003, '2099-12-03', '09:00', '12:00', 5, TRUE);

INSERT INTO slot_divers (slot_id, first_name, last_name, level, email, is_director, registration_status)
VALUES (1003, 'Directeur', 'PLONGEE', 'MF2', 'dp@test.com', TRUE, 'CONFIRMED');

-- 1004 : Inscriptions activées, dp@test.com directeur, 1 place PLEINE (créneau complet)
INSERT INTO dive_slots (id, slot_date, start_time, end_time, diver_count, registration_enabled)
VALUES (1004, '2099-12-04', '09:00', '12:00', 1, TRUE);

INSERT INTO slot_divers (slot_id, first_name, last_name, level, email, is_director, registration_status)
VALUES (1004, 'Directeur', 'PLONGEE', 'MF2', 'dp@test.com', TRUE, 'CONFIRMED');

INSERT INTO slot_divers (slot_id, first_name, last_name, level, email, is_director, registration_status, registered_user_id)
VALUES (1004, 'Plongeur', 'CONFIRM', 'N2', 'other@test.com', FALSE, 'CONFIRMED', 1005);

-- 1005 : Inscriptions activées, dp@test.com directeur, diver@test.com PENDING
--        → utilisé pour tester file d'attente, validation, annulation
INSERT INTO dive_slots (id, slot_date, start_time, end_time, diver_count, registration_enabled)
VALUES (1005, '2099-12-05', '09:00', '12:00', 5, TRUE);

INSERT INTO slot_divers (slot_id, first_name, last_name, level, email, is_director, registration_status)
VALUES (1005, 'Directeur', 'PLONGEE', 'MF2', 'dp@test.com', TRUE, 'CONFIRMED');

INSERT INTO slot_divers (slot_id, first_name, last_name, level, email, is_director,
                         registration_status, registered_user_id, number_of_dives, last_dive_date)
VALUES (1005, 'Plongeur', 'TEST', 'N2', 'diver@test.com', FALSE,
        'PENDING', 1004, 15, '2024-06-01');
