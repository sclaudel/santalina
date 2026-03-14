-- Resynchronisation des séquences IDENTITY H2.
-- Nécessaire lorsque des données ont été insérées avec des IDs explicites
-- (ex. import depuis un dump PostgreSQL) sans avancer la séquence interne H2.
-- Cette migration s'exécute uniquement pour les profils H2 (dev / test).

ALTER TABLE slot_divers  ALTER COLUMN id RESTART WITH 100000;
ALTER TABLE dive_slots   ALTER COLUMN id RESTART WITH 100000;
ALTER TABLE users        ALTER COLUMN id RESTART WITH 100000;
ALTER TABLE palanquees   ALTER COLUMN id RESTART WITH 100000;
