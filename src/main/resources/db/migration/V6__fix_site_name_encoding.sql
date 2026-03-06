-- V6 : Correction encodage du nom du site dans app_config
-- La valeur était corrompue (double-encodage Latin-1/UTF-8)
-- On supprime la clé pour qu'elle soit réinitialisée correctement au démarrage
DELETE FROM app_config WHERE config_key = 'site.name';

