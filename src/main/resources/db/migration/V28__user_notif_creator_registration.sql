-- V28 : Notification distincte pour le créateur de créneau (vs DP assigné)
-- notif_on_creator_registration : désactivée par défaut (le créateur n'est pas forcément le DP)
ALTER TABLE users
    ADD COLUMN notif_on_creator_registration BOOLEAN NOT NULL DEFAULT FALSE;
