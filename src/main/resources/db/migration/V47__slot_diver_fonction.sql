-- V47: Ajoute le champ 'fonction' à la table slot_divers (permet de modifier la fonction avant assignation à une palanquée)
ALTER TABLE slot_divers ADD COLUMN fonction VARCHAR(50);
