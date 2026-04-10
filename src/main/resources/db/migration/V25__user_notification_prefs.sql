-- Préférences de notifications par e-mail pour chaque utilisateur
ALTER TABLE users ADD COLUMN notif_on_registration     BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN notif_on_approved         BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN notif_on_cancelled        BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN notif_on_moved_to_waitlist BOOLEAN NOT NULL DEFAULT TRUE;
