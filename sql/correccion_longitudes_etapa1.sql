-- =====================================================================
-- LTOTRACE · Corrección de longitudes de columnas
-- Con Spring Boot 4 (Hibernate 7) y spring.jpa.hibernate.ddl-auto=update,
-- Hibernate amplió a VARCHAR(255) las columnas de las entidades Cinta y Cliente
-- que no tenían longitud declarada. Este script las devuelve a las longitudes
-- del script de creación y del diccionario de datos.
-- Ejecutar con LTOTRACE detenido y DESPUÉS de cambiar ddl-auto a none.
-- =====================================================================

-- 0) CONTROL (solo lectura): debe devolver 0 en todas las columnas.
--    Si algún valor es mayor que 0, hay datos más largos que el límite: avisar antes de seguir.
-- SELECT
--   COUNT(*) FILTER (WHERE LENGTH(barcode)    > 50) AS barcode_largo,
--   COUNT(*) FILTER (WHERE LENGTH(volume_id)  > 20) AS volume_id_largo,
--   COUNT(*) FILTER (WHERE LENGTH(ubicacion)  > 50) AS ubicacion_larga,
--   COUNT(*) FILTER (WHERE LENGTH(dia_semana) > 20) AS dia_semana_largo
-- FROM cintas;
-- SELECT COUNT(*) FILTER (WHERE LENGTH(nombre) > 100) AS nombre_largo FROM clientes;

BEGIN;

ALTER TABLE cintas
    ALTER COLUMN barcode    TYPE VARCHAR(50),
    ALTER COLUMN volume_id  TYPE VARCHAR(20),
    ALTER COLUMN ubicacion  TYPE VARCHAR(50),
    ALTER COLUMN dia_semana TYPE VARCHAR(20),
    ALTER COLUMN tecnologia TYPE VARCHAR(20);

ALTER TABLE clientes
    ALTER COLUMN nombre TYPE VARCHAR(100);

COMMIT;

-- Verificación: muestra el tipo y la longitud final de cada columna
-- SELECT table_name, column_name, data_type, character_maximum_length
--   FROM information_schema.columns
--  WHERE table_name IN ('cintas', 'clientes')
--  ORDER BY table_name, ordinal_position;
