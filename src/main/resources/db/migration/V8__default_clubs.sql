-- V8 : Clubs par défaut
INSERT INTO app_config (config_key, config_value, updated_at)
SELECT 'slot.clubs', 'CLUB - APNEE|CLUB - MILLE BULLES|CLUB - COMPAGNONS DE NEPTUNE|CLUB - CRENSOA|CLUB - OCTOPUS CELLOIS', NOW()
WHERE NOT EXISTS (SELECT 1 FROM app_config WHERE config_key = 'slot.clubs');
