-- V45: Ajoute le champ 'fonction' à la table free_palanquee_members (miroir de palanquee_members.fonction)
ALTER TABLE free_palanquee_members ADD COLUMN fonction VARCHAR(50);
