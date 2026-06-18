-- V46: Ajoute le champ 'fonction' à la table free_session_divers (permet de modifier la fonction avant assignation)
ALTER TABLE free_session_divers ADD COLUMN fonction VARCHAR(50);
