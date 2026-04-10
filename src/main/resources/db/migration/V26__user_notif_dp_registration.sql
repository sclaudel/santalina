-- Préférence utilisateur : recevoir les notifications d'inscription sur ses créneaux (vue DP/créateur)
ALTER TABLE users ADD COLUMN notif_on_dp_registration BOOLEAN NOT NULL DEFAULT TRUE;
