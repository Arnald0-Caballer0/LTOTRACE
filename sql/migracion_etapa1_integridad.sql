-- =====================================================================
-- LTOTRACE · Migración de integridad y seguridad del modelo de datos
-- Base de datos: atopa (PostgreSQL)
--
-- Ejecutar con el superusuario (postgres) o con el dueño de la base:
--   psql -h localhost -U postgres -d atopa -f migracion_etapa1_integridad.sql
--
-- Antes de ejecutar:
--   1) Hacer un respaldo:  pg_dump -h localhost -U postgres -F c -f atopa_antes_migracion.backup atopa
--   2) Ejecutar las CONSULTAS DE CONTROL (sección 0) y revisar sus resultados.
-- Todo el script corre en una sola transacción: si algo falla, no se aplica nada.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0) CONSULTAS DE CONTROL (solo lectura; ejecutarlas antes, una por una)
-- ---------------------------------------------------------------------
-- a) Tecnologías de cintas que no existen en el catálogo (debe quedar vacío
--    después de la normalización del punto 2):
--    SELECT DISTINCT tecnologia FROM cintas
--     WHERE tecnologia IS NOT NULL
--       AND UPPER(TRIM(tecnologia)) NOT IN (SELECT UPPER(TRIM(nombre)) FROM tecnologias_lto);
--
-- b) Volume ID repetidos (debe dar 0 filas; si aparece alguno, reiniciar el
--    Volume ID de la cinta incorrecta desde LTOTRACE y volver a relacionarla):
--    SELECT volume_id, STRING_AGG(barcode, ', ') AS cintas, COUNT(*)
--      FROM cintas
--     WHERE volume_id IS NOT NULL AND volume_id <> 'PENDIENTE'
--     GROUP BY volume_id HAVING COUNT(*) > 1;
--
-- c) Ubicaciones registradas (solo deben existir variantes de ARMARIO, LIBRERIA
--    o RETIRADO; cualquier otro valor debe corregirse antes de migrar):
--    SELECT ubicacion, COUNT(*) FROM cintas GROUP BY ubicacion;
--
-- d) Tipos de movimiento registrados (solo deben aparecer los del punto 5;
--    MOVIMIENTO_FISICO corresponde a registros de versiones anteriores):
--    SELECT tipo_movimiento, COUNT(*) FROM movimientos GROUP BY tipo_movimiento;
-- ---------------------------------------------------------------------

BEGIN;

-- ---------------------------------------------------------------------
-- 1) Principio de mínimo privilegio: un usuario nuevo nace sin capacidades
--    operativas ni alcance sobre clientes (no modifica a los usuarios existentes)
-- ---------------------------------------------------------------------
ALTER TABLE usuarios ALTER COLUMN ve_todos_clientes    SET DEFAULT FALSE;
ALTER TABLE usuarios ALTER COLUMN puede_mover_cintas   SET DEFAULT FALSE;
ALTER TABLE usuarios ALTER COLUMN puede_crear_cintas   SET DEFAULT FALSE;
ALTER TABLE usuarios ALTER COLUMN puede_retirar_cintas SET DEFAULT FALSE;

-- ---------------------------------------------------------------------
-- 2) Tecnología de la cinta como relación con su tabla
--    (clave foránea sobre el nombre, que es único en tecnologias_lto)
-- ---------------------------------------------------------------------
UPDATE cintas c
   SET tecnologia = t.nombre
  FROM tecnologias_lto t
 WHERE UPPER(TRIM(c.tecnologia)) = UPPER(TRIM(t.nombre))
   AND c.tecnologia <> t.nombre;

ALTER TABLE cintas
  ADD CONSTRAINT fk_cintas_tecnologia
  FOREIGN KEY (tecnologia) REFERENCES tecnologias_lto (nombre)
  ON UPDATE CASCADE ON DELETE RESTRICT;

-- ---------------------------------------------------------------------
-- 3) Usuario de cada movimiento como relación con la tabla usuarios
-- ---------------------------------------------------------------------
-- 3.1 Usuario técnico "sistema" para las operaciones internas (no puede iniciar sesión)
INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, activo, fecha_creacion,
                      ve_todos_clientes, puede_mover_cintas, puede_crear_cintas, puede_retirar_cintas)
SELECT 'sistema', 'SIN-ACCESO', 'Usuario técnico del sistema',
       (SELECT id FROM roles WHERE codigo = 'OPERADOR'), FALSE, CURRENT_TIMESTAMP,
       FALSE, FALSE, FALSE, FALSE
 WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'sistema');

-- 3.2 Conserva el historial: si algún movimiento antiguo tiene un usuario que ya no
--     existe, se lo registra como usuario histórico inactivo (sin acceso)
INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, activo, fecha_creacion,
                      ve_todos_clientes, puede_mover_cintas, puede_crear_cintas, puede_retirar_cintas)
SELECT DISTINCT m.usuario, 'SIN-ACCESO', 'Usuario histórico',
       (SELECT id FROM roles WHERE codigo = 'OPERADOR'), FALSE, CURRENT_TIMESTAMP,
       FALSE, FALSE, FALSE, FALSE
  FROM movimientos m
 WHERE m.usuario IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.username = m.usuario);

ALTER TABLE movimientos
  ADD CONSTRAINT fk_movimientos_usuario
  FOREIGN KEY (usuario) REFERENCES usuarios (username)
  ON UPDATE RESTRICT ON DELETE RESTRICT;   -- el nombre de usuario no se modifica ni se elimina (baja lógica)

-- ---------------------------------------------------------------------
-- 4) Un mismo Volume ID no puede estar vinculado a dos cintas
--    (PENDIENTE es el valor inicial de todas las cintas sin vincular)
-- ---------------------------------------------------------------------
CREATE UNIQUE INDEX ux_cintas_volume_id
    ON cintas (volume_id)
 WHERE volume_id IS NOT NULL AND volume_id <> 'PENDIENTE';

-- ---------------------------------------------------------------------
-- 5) Catálogo de ubicaciones y de tipos de movimiento
-- ---------------------------------------------------------------------
UPDATE cintas SET ubicacion = 'LIBRERIA' WHERE UPPER(TRIM(ubicacion)) IN ('LIBRERIA', 'LIBRERÍA');
UPDATE cintas SET ubicacion = 'ARMARIO'  WHERE UPPER(TRIM(ubicacion)) = 'ARMARIO';
UPDATE cintas SET ubicacion = 'RETIRADO' WHERE UPPER(TRIM(ubicacion)) IN ('RETIRADO', 'RETIRADA');
UPDATE cintas SET ubicacion = 'ARMARIO'  WHERE ubicacion IS NULL;

ALTER TABLE cintas ALTER COLUMN ubicacion SET NOT NULL;
ALTER TABLE cintas
  ADD CONSTRAINT ck_cintas_ubicacion
  CHECK (ubicacion IN ('ARMARIO', 'LIBRERIA', 'RETIRADO'));

ALTER TABLE movimientos
  ADD CONSTRAINT ck_movimientos_tipo
  CHECK (tipo_movimiento IN ('ALTA', 'RELACION', 'REINICIO', 'MONTAJE', 'DESMONTAJE',
                             'CAMBIO_UBICACION', 'MARCA_SEMANAL', 'DESMARCA_SEMANAL', 'RETIRO',
                             'MOVIMIENTO_FISICO'));  -- MOVIMIENTO_FISICO: tipo histórico de versiones anteriores

-- ---------------------------------------------------------------------
-- 6) El estado de la cinta es derivado (PENDIENTE / VINCULADA / RETIRADA):
--    se calcula a partir del Volume ID y la ubicación, por lo que la columna
--    "estado", que nunca se completaba, se elimina
-- ---------------------------------------------------------------------
ALTER TABLE cintas DROP COLUMN IF EXISTS estado;

-- ---------------------------------------------------------------------
-- 7) Registros de auditoría e historial inmodificables
--    Ni la aplicación ni un usuario de la base pueden modificar o borrar
--    eventos de auditoría, conciliaciones ni movimientos ya registrados.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_registro_inmutable()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de la tabla % no pueden modificarse ni eliminarse (registro inmutable).',
        TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_auditoria_inmutable
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();

CREATE TRIGGER trg_auditoria_inventario_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();

CREATE TRIGGER trg_auditoria_inventario_detalle_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_inventario_detalle
    FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();

CREATE TRIGGER trg_movimientos_inmutable
    BEFORE UPDATE OR DELETE ON movimientos
    FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();

-- Si la aplicación usa el usuario dedicado ltotrace_app (Manual Técnico, Paso 2),
-- además se le quitan los privilegios de modificación sobre esas tablas.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ltotrace_app') THEN
        REVOKE UPDATE, DELETE, TRUNCATE
            ON auditoria, auditoria_inventario, auditoria_inventario_detalle, movimientos
          FROM ltotrace_app;
    END IF;
END;
$$;

COMMIT;

-- Verificación posterior (opcional):
--   UPDATE auditoria SET detalle = 'x' WHERE id = (SELECT MIN(id) FROM auditoria);
--   -> debe fallar con: "Los registros de la tabla auditoria no pueden modificarse..."
