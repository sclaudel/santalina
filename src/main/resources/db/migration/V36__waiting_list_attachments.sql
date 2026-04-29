-- Ajout du statut d'inscription et des chemins de pièces jointes sur waiting_list_entries
ALTER TABLE waiting_list_entries ADD COLUMN registration_status VARCHAR(30) DEFAULT 'PENDING_VERIFICATION' NOT NULL;
ALTER TABLE waiting_list_entries ADD COLUMN rejection_reason TEXT;
ALTER TABLE waiting_list_entries ADD COLUMN medical_cert_path VARCHAR(500);
ALTER TABLE waiting_list_entries ADD COLUMN license_qr_path VARCHAR(500);
ALTER TABLE waiting_list_entries ADD COLUMN attachments_deleted_at TIMESTAMP;
