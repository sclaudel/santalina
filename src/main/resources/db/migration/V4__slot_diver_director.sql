-- V4: Ajout des champs email et téléphone sur les plongeurs inscrits
ALTER TABLE slot_divers ADD COLUMN IF NOT EXISTS email    VARCHAR(150);
ALTER TABLE slot_divers ADD COLUMN IF NOT EXISTS phone    VARCHAR(30);
ALTER TABLE slot_divers ADD COLUMN IF NOT EXISTS is_director BOOLEAN NOT NULL DEFAULT FALSE;

