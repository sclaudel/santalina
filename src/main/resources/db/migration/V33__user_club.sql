-- V33 : Ajout du club d'appartenance sur le compte utilisateur
ALTER TABLE users ADD COLUMN club VARCHAR(255);
