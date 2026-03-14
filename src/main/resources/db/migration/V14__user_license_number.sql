-- Ajout du numéro de licence fédérale (FFESSM ou autre)
ALTER TABLE users ADD COLUMN license_number VARCHAR(50);
