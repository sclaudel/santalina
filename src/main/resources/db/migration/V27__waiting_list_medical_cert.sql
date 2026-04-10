-- V27 : Champs de conformité médicale et licence sur la liste d'attente
-- medical_cert_date  : date de début (début de validité) du certificat médical
-- license_confirmed  : case à cocher FFESSM cochée par le plongeur lors de l'inscription
ALTER TABLE waiting_list_entries ADD COLUMN medical_cert_date DATE;
ALTER TABLE waiting_list_entries ADD COLUMN license_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
