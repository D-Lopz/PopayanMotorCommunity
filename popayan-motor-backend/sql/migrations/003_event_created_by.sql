-- v1.3: guarda qué administrador creó cada evento (contador "Eventos" del perfil del admin).
-- NO destructiva: solo AÑADE la columna event.created_by (NULL), su índice y su clave foránea.
-- No modifica ni borra filas: los eventos existentes quedan con created_by = NULL (autor desconocido)
-- y no cuentan para ningún administrador.
--
-- Ejecutar sobre la base actual (mysql CLI o MySQL Workbench, ambos entienden DELIMITER):
--   mysql -u root -p popayan_motor < sql/migrations/003_event_created_by.sql
--
-- Se puede ejecutar más de una vez y retomar si se cortó a mitad: cada paso (columna, índice, FK)
-- se comprueba por separado en information_schema y solo se crea lo que falta. Si algo ya existe
-- con OTRA definición, se detiene con un error (SIGNAL) sin tocar nada más, para revisarlo a mano.
-- Al terminar se borra el procedimiento auxiliar.
USE popayan_motor;

DROP PROCEDURE IF EXISTS migration_003_event_created_by;

DELIMITER //
CREATE PROCEDURE migration_003_event_created_by()
BEGIN
  DECLARE member_id_type VARCHAR(64);
  DECLARE col_type VARCHAR(64);
  DECLARE col_nullable VARCHAR(3);
  DECLARE col_default TEXT;
  DECLARE idx_columns TEXT;
  DECLARE idx_non_unique INT;
  DECLARE fk_ref_table VARCHAR(64);
  DECLARE fk_ref_column VARCHAR(64);
  DECLARE fk_delete_rule VARCHAR(64);
  DECLARE fk_columns TEXT;
  DECLARE other_fks INT;

  -- El tipo de created_by debe ser idéntico al de member.id para que la FK sea válida.
  SELECT COLUMN_TYPE INTO member_id_type FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'member' AND COLUMN_NAME = 'id';

  -- ── 1. Columna ────────────────────────────────────────────────
  SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT INTO col_type, col_nullable, col_default
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event' AND COLUMN_NAME = 'created_by';
  IF col_type IS NULL THEN
    ALTER TABLE event ADD COLUMN created_by INT NULL AFTER distance_km;
  ELSEIF col_type <> member_id_type OR col_nullable <> 'YES' OR col_default IS NOT NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'event.created_by ya existe con otra definicion (se esperaba INT NULL sin default). Revisar a mano.';
  END IF;

  -- ── 2. Índice ─────────────────────────────────────────────────
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX), MAX(NON_UNIQUE) INTO idx_columns, idx_non_unique
    FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event' AND INDEX_NAME = 'idx_event_created_by';
  IF idx_columns IS NULL THEN
    CREATE INDEX idx_event_created_by ON event (created_by);
  ELSEIF idx_columns <> 'created_by' OR idx_non_unique <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'idx_event_created_by ya existe con otra definicion (se esperaba indice no unico sobre created_by). Revisar a mano.';
  END IF;

  -- ── 3. Clave foránea ──────────────────────────────────────────
  -- Otra FK sobre created_by con distinto nombre también es una estructura inesperada.
  SELECT COUNT(*) INTO other_fks FROM information_schema.KEY_COLUMN_USAGE
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event' AND COLUMN_NAME = 'created_by'
     AND REFERENCED_TABLE_NAME IS NOT NULL AND CONSTRAINT_NAME <> 'fk_event_created_by';
  IF other_fks > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'event.created_by ya tiene otra clave foranea con otro nombre. Revisar a mano.';
  END IF;

  SELECT rc.REFERENCED_TABLE_NAME, rc.DELETE_RULE INTO fk_ref_table, fk_delete_rule
    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
   WHERE rc.CONSTRAINT_SCHEMA = DATABASE() AND rc.TABLE_NAME = 'event' AND rc.CONSTRAINT_NAME = 'fk_event_created_by';
  IF fk_ref_table IS NULL THEN
    ALTER TABLE event ADD CONSTRAINT fk_event_created_by
      FOREIGN KEY (created_by) REFERENCES member (id) ON DELETE SET NULL;
  ELSE
    SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION), MAX(REFERENCED_COLUMN_NAME)
      INTO fk_columns, fk_ref_column
      FROM information_schema.KEY_COLUMN_USAGE
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event' AND CONSTRAINT_NAME = 'fk_event_created_by';
    IF fk_ref_table <> 'member' OR fk_ref_column <> 'id' OR fk_columns <> 'created_by' OR fk_delete_rule <> 'SET NULL' THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'fk_event_created_by ya existe con otra definicion (se esperaba created_by -> member(id) ON DELETE SET NULL). Revisar a mano.';
    END IF;
  END IF;
END //
DELIMITER ;

CALL migration_003_event_created_by();
DROP PROCEDURE IF EXISTS migration_003_event_created_by;

-- Resumen: los eventos siguen siendo los mismos; los anteriores quedan sin autor.
SELECT COUNT(*) AS eventos, SUM(created_by IS NULL) AS sin_autor FROM event;

-- ── Asignación manual OPCIONAL (NO se ejecuta) ──────────────────
-- Solo si se CONFIRMA quién creó ciertos eventos antiguos. Reemplazar <ID_ADMIN> y la lista de ids:
-- UPDATE event SET created_by = <ID_ADMIN>
--  WHERE created_by IS NULL AND id IN (<ids confirmados>)
--    AND (SELECT role FROM member WHERE id = <ID_ADMIN>) = 'ADMINISTRATOR';
