-- Ajout du numéro de licence sur les plongeurs d'un créneau (utilisé pour le DP)
ALTER TABLE slot_divers ADD COLUMN license_number VARCHAR(50);
