-- Aptitudes spécifiques à une plongée pour un plongeur dans une palanquée.
-- Si NULL, on utilise les aptitudes globales du slot_diver.
ALTER TABLE palanquee_members ADD COLUMN aptitudes VARCHAR(255);
