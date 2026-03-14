-- V12: Séparation du champ 'name' en 'first_name' et 'last_name'

ALTER TABLE users ADD COLUMN first_name VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN last_name  VARCHAR(100) NOT NULL DEFAULT '';

UPDATE users SET
  first_name = CASE
    WHEN POSITION(' ' IN name) > 0
      THEN SUBSTR(name, 1, POSITION(' ' IN name) - 1)
    ELSE name
  END,
  last_name = CASE
    WHEN POSITION(' ' IN name) > 0
      THEN LTRIM(SUBSTR(name, POSITION(' ' IN name) + 1))
    ELSE ''
  END;

ALTER TABLE users DROP COLUMN name;
