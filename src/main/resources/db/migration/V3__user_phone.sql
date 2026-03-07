-- V3: Ajout du numéro de téléphone sur les utilisateurs
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(30);

