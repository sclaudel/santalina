-- V23 : Ajout du système d'inscription en file d'attente

-- Colonnes dans dive_slots pour activer/désactiver les inscriptions
ALTER TABLE dive_slots
    ADD COLUMN registration_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE dive_slots
    ADD COLUMN registration_opens_at TIMESTAMP;

-- Colonnes dans slot_divers pour le suivi des inscriptions
ALTER TABLE slot_divers
    ADD COLUMN registration_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE slot_divers
    ADD COLUMN registered_user_id BIGINT;

ALTER TABLE slot_divers
    ADD COLUMN number_of_dives INTEGER;

ALTER TABLE slot_divers
    ADD COLUMN last_dive_date DATE;

ALTER TABLE slot_divers
    ADD COLUMN prepared_level VARCHAR(100);

ALTER TABLE slot_divers
    ADD COLUMN registration_comment TEXT;

ALTER TABLE slot_divers
    ADD COLUMN registration_validated_at TIMESTAMP;

-- Index pour trier la file d'attente efficacement
CREATE INDEX IF NOT EXISTS idx_slot_divers_slot_status_added
    ON slot_divers(slot_id, registration_status, added_at);

CREATE INDEX IF NOT EXISTS idx_slot_divers_slot_registered_user
    ON slot_divers(slot_id, registered_user_id);
