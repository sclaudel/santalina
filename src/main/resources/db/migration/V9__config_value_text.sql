-- V9 : Agrandissement de config_value de VARCHAR(255) à TEXT
--      pour permettre des listes de clubs/types longues
ALTER TABLE app_config ALTER COLUMN config_value TYPE TEXT;
