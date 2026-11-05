-- LTOTRACE · Prueba de las restricciones de integridad (no modifica datos)
-- Cada intento se ejecuta en un bloque protegido: si la base lo rechaza, se informa
-- el mensaje; si lo aceptara, se deshace igual y se informa como FALLA.
DO $$
DECLARE
    v_tec  TEXT;  v_bc1 TEXT;  v_bc2 TEXT;  v_vol TEXT;
    v_aud  BIGINT; v_mov BIGINT;

BEGIN
    SELECT tecnologia INTO v_tec FROM cintas WHERE tecnologia IS NOT NULL LIMIT 1;
    SELECT barcode, volume_id INTO v_bc1, v_vol FROM cintas
     WHERE volume_id IS NOT NULL AND volume_id <> 'PENDIENTE' ORDER BY barcode LIMIT 1;
    SELECT barcode INTO v_bc2 FROM cintas
     WHERE volume_id IS NOT NULL AND volume_id <> 'PENDIENTE' ORDER BY barcode DESC LIMIT 1;
    SELECT MIN(id) INTO v_aud FROM auditoria;
    SELECT MIN(id) INTO v_mov FROM movimientos;

    -- a) Eliminar una tecnología asignada a cintas
    BEGIN
        DELETE FROM tecnologias_lto WHERE nombre = v_tec;
        RAISE EXCEPTION 'FALLA';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'a) Eliminar la tecnología % en uso -> %', v_tec,
            CASE WHEN SQLERRM = 'FALLA' THEN 'FALLA: la base lo permitió' ELSE 'RECHAZADO: ' || SQLERRM END;
    END;

    -- b) Ubicación fuera de catálogo
    BEGIN
        UPDATE cintas SET ubicacion = 'OFICINA' WHERE barcode = v_bc1;
        RAISE EXCEPTION 'FALLA';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'b) Ubicación OFICINA en la cinta % -> %', v_bc1,
            CASE WHEN SQLERRM = 'FALLA' THEN 'FALLA: la base lo permitió' ELSE 'RECHAZADO: ' || SQLERRM END;
    END;

    -- c) Mismo Volume ID en dos cintas
    BEGIN
        UPDATE cintas SET volume_id = v_vol WHERE barcode = v_bc2;
        RAISE EXCEPTION 'FALLA';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'c) Volume ID % de la cinta % asignado a la cinta % -> %', v_vol, v_bc1, v_bc2,
            CASE WHEN SQLERRM = 'FALLA' THEN 'FALLA: la base lo permitió' ELSE 'RECHAZADO: ' || SQLERRM END;
    END;

    -- d) Tipo de movimiento fuera de catálogo
    BEGIN
        INSERT INTO movimientos (barcode, tipo_movimiento, usuario) VALUES (v_bc1, 'OTRO', 'sistema');
        RAISE EXCEPTION 'FALLA';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'd) Movimiento de tipo OTRO -> %',
            CASE WHEN SQLERRM = 'FALLA' THEN 'FALLA: la base lo permitió' ELSE 'RECHAZADO: ' || SQLERRM END;
    END;

    -- e) Modificar un registro de auditoría
    BEGIN
        UPDATE auditoria SET detalle = 'x' WHERE id = v_aud;
        RAISE EXCEPTION 'FALLA';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'e) Modificar el evento de auditoría % -> %', v_aud,
            CASE WHEN SQLERRM = 'FALLA' THEN 'FALLA: la base lo permitió' ELSE 'RECHAZADO: ' || SQLERRM END;
    END;

    -- f) Eliminar un movimiento
    BEGIN
        DELETE FROM movimientos WHERE id = v_mov;
        RAISE EXCEPTION 'FALLA';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'f) Eliminar el movimiento % -> %', v_mov,
            CASE WHEN SQLERRM = 'FALLA' THEN 'FALLA: la base lo permitió' ELSE 'RECHAZADO: ' || SQLERRM END;
    END;
END;
$$;
