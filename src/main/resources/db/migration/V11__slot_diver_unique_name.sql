-- V11: Contrainte d'unicité sur (slot_id, first_name, last_name) pour éviter les doublons de plongeurs sur un créneau

-- Suppression des doublons éventuels : on conserve l'entrée avec le plus petit id pour chaque combinaison (slot_id, first_name, last_name)
DELETE FROM slot_divers
WHERE id NOT IN (
    SELECT MIN(id)
    FROM slot_divers
    GROUP BY slot_id, first_name, last_name
);

-- Ajout de la contrainte d'unicité
ALTER TABLE slot_divers ADD CONSTRAINT uq_slot_diver_name UNIQUE (slot_id, first_name, last_name);
