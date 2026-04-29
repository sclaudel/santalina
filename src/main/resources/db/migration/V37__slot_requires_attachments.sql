-- Activation des pièces jointes obligatoires sur certains créneaux en inscription libre
ALTER TABLE dive_slots ADD COLUMN requires_attachments BOOLEAN DEFAULT FALSE NOT NULL;
