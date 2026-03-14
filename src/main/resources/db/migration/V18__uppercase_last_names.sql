-- V18 : Mise en majuscules des noms de famille existants
-- S'applique à la fois sur H2 (dev/test) et PostgreSQL (prod).
-- UPPER() est standard SQL, compatible avec les deux moteurs.

UPDATE slot_divers SET last_name = UPPER(last_name);
UPDATE users       SET last_name = UPPER(last_name);
