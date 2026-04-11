-- V31 : Club d'appartenance du plongeur dans la liste d'attente
-- Permet au plongeur de préciser son club lors de l'inscription libre
ALTER TABLE waiting_list_entries ADD COLUMN club VARCHAR(255);
