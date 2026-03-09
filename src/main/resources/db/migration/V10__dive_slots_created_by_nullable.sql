-- V10 : Rendre created_by nullable dans dive_slots avec ON DELETE SET NULL
--       Permet de supprimer un utilisateur même s'il a créé des créneaux
ALTER TABLE dive_slots DROP CONSTRAINT IF EXISTS constraint_f9;
ALTER TABLE dive_slots DROP CONSTRAINT IF EXISTS fk_dive_slots_created_by;

-- Supprimer toutes les FK existantes vers users.id sur cette table (H2)
ALTER TABLE dive_slots ALTER COLUMN created_by DROP NOT NULL;

ALTER TABLE dive_slots ADD CONSTRAINT fk_dive_slots_created_by
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;
