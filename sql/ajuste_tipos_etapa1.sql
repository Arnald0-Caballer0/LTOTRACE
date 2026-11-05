-- =====================================================================
-- LTOTRACE · Ajuste de tipos de datos (complemento de la migración de la Etapa 1)
-- Alinea la base atopa con el script de creación y el diccionario de datos:
--   - cintas.tecnologia pasa a VARCHAR(20), igual que tecnologias_lto.nombre (su clave foránea)
--   - movimientos.id pasa a BIGINT, igual que el resto de las claves primarias
-- Ejecutar DESPUÉS de migracion_etapa1_integridad.sql:
--   psql -h localhost -U postgres -d atopa -f ajuste_tipos_etapa1.sql
-- =====================================================================
BEGIN;

ALTER TABLE cintas      ALTER COLUMN tecnologia TYPE VARCHAR(20);

ALTER TABLE movimientos ALTER COLUMN id TYPE BIGINT;
DO $$
DECLARE secuencia TEXT := pg_get_serial_sequence('movimientos', 'id');
BEGIN
    IF secuencia IS NOT NULL THEN
        EXECUTE format('ALTER SEQUENCE %s AS BIGINT', secuencia);
    END IF;
END;
$$;

COMMIT;

-- Verificación: debe mostrar tecnologia = character varying(20) e id = bigint
--   SELECT table_name, column_name, data_type, character_maximum_length
--     FROM information_schema.columns
--    WHERE (table_name = 'cintas' AND column_name = 'tecnologia')
--       OR (table_name = 'movimientos' AND column_name = 'id');
