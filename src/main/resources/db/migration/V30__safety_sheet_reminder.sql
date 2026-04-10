-- V30: Rappel fiche de sécurité pour le DP après la sortie
ALTER TABLE dive_slots ADD COLUMN IF NOT EXISTS reminder_sent_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notif_on_safety_reminder BOOLEAN NOT NULL DEFAULT TRUE;
