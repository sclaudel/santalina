-- Ajout du champ club_certified : indique que l'utilisateur a certifié sur l'honneur être membre du club indiqué
ALTER TABLE users ADD COLUMN club_certified BOOLEAN NOT NULL DEFAULT FALSE;
