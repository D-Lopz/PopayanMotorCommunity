-- v1.2: agrega la ciudad al perfil del miembro.
-- NO destructiva: solo añade una columna opcional; los miembros existentes quedan con city = NULL
-- y la completan después desde "Editar perfil". Ejecutar UNA vez sobre la base actual:
--   mysql -u root -p popayan_motor < sql/migrations/001_member_city.sql
-- (Si se ejecuta dos veces, MySQL responde "Duplicate column name 'city'" y no cambia nada.)
USE popayan_motor;

ALTER TABLE member ADD COLUMN city VARCHAR(100) NULL AFTER phone;
