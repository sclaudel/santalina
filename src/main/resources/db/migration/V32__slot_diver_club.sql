-- V32 : Club d'appartenance du plongeur dans les inscrits d'un créneau
-- Transféré depuis la liste d'attente lors de la validation, ou saisi manuellement par le DP
ALTER TABLE slot_divers ADD COLUMN club VARCHAR(255);
