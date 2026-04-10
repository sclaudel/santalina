-- V29 : Date de certificat médical et commentaire sur les plongeurs des créneaux
-- Copiés depuis waiting_list_entries lors de la validation (approve)
ALTER TABLE slot_divers ADD COLUMN IF NOT EXISTS medical_cert_date DATE;
ALTER TABLE slot_divers ADD COLUMN IF NOT EXISTS comment TEXT;
