-- V5 : Multi-rôles par utilisateur + renommage GUEST -> DIVER
-- 1. Créer la table de jointure user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id  BIGINT       NOT NULL,
    role     VARCHAR(50)  NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 2. Migrer les rôles existants (colonne users.role) vers user_roles
--    en renommant GUEST -> DIVER au passage
INSERT INTO user_roles (user_id, role)
SELECT id,
       CASE WHEN role = 'GUEST' THEN 'DIVER' ELSE role END
FROM users;

-- 3. Mettre à jour la colonne users.role pour cohérence (GUEST -> DIVER)
UPDATE users SET role = 'DIVER' WHERE role = 'GUEST';

-- NB : on conserve users.role comme "rôle principal" pour compatibilité ascendante
--      La source de vérité pour les droits est désormais user_roles

