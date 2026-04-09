-- V24 — Rend number_of_dives et last_dive_date facultatifs dans waiting_list_entries.
-- Ces informations ne sont plus collectées lors de l'inscription libre.
ALTER TABLE waiting_list_entries ALTER COLUMN number_of_dives DROP NOT NULL;
ALTER TABLE waiting_list_entries ALTER COLUMN last_dive_date  DROP NOT NULL;
