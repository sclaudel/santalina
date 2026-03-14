-- V13: Mécanisme d'activation de compte par email
-- Les nouvelles inscriptions créent un compte inactif ; l'utilisateur clique sur
-- un lien reçu par email pour choisir son mot de passe et activer son compte.

-- Comptes existants : déjà activés par défaut
ALTER TABLE users ADD COLUMN activated BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN activation_token VARCHAR(255);
ALTER TABLE users ADD COLUMN activation_token_expiry TIMESTAMP;

-- Le hash du mot de passe peut être nul pendant que le compte attend son activation
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
