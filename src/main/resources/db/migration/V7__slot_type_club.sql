-- V7 : Ajout du type de créneau et du club sur dive_slots
ALTER TABLE dive_slots ADD COLUMN IF NOT EXISTS slot_type VARCHAR(100);
ALTER TABLE dive_slots ADD COLUMN IF NOT EXISTS club      VARCHAR(100);

