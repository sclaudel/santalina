-- V16: Position des plongeurs au sein d'une palanquée (pour l'ordre d'affichage)
ALTER TABLE slot_divers ADD COLUMN IF NOT EXISTS palanquee_position INT NOT NULL DEFAULT 0;
