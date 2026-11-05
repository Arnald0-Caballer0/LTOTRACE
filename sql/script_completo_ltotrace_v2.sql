-- ============================================================
-- LTOTRACE — Script de creación de la base de datos (PostgreSQL)
-- 11 tablas · Coherente con el diccionario de datos
-- Versión 2: integridad referencial, catálogos y registros inmutables
-- ============================================================

-- 1) Tablas sin dependencias

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(20) UNIQUE NOT NULL,
    nombre      VARCHAR(50) NOT NULL,
    descripcion TEXT
);

CREATE TABLE clientes (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    encargado           VARCHAR(255),
    telefono            VARCHAR(255),
    email               VARCHAR(255),
    fecha_registro      DATE,
    promedio_gb_backup  INTEGER DEFAULT 0,
    dias_entre_backups  INTEGER DEFAULT 0,
    activo              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tecnologias_lto (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(20) UNIQUE NOT NULL,
    generacion   INTEGER,
    capacidad_gb INTEGER NOT NULL
);

CREATE TABLE configuracion (
    clave VARCHAR(60) PRIMARY KEY,
    valor TEXT
);

-- 2) Tablas con dependencias

CREATE TABLE usuarios (
    id                    BIGSERIAL PRIMARY KEY,
    username              VARCHAR(50) UNIQUE NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    nombre_completo       VARCHAR(120),
    rol_id                BIGINT REFERENCES roles(id),
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ve_todos_clientes     BOOLEAN DEFAULT FALSE,
    puede_mover_cintas    BOOLEAN DEFAULT FALSE,
    puede_crear_cintas    BOOLEAN DEFAULT FALSE,
    puede_retirar_cintas  BOOLEAN DEFAULT FALSE,
    intentos_fallidos     INTEGER DEFAULT 0,
    bloqueado_hasta       TIMESTAMP
);

CREATE TABLE cintas (
    barcode             VARCHAR(50) PRIMARY KEY,
    volume_id           VARCHAR(20),
    cliente_id          BIGINT REFERENCES clientes(id),
    ubicacion           VARCHAR(50) NOT NULL DEFAULT 'ARMARIO'
                        CONSTRAINT ck_cintas_ubicacion CHECK (ubicacion IN ('ARMARIO', 'LIBRERIA', 'RETIRADO')),
    tecnologia          VARCHAR(20) CONSTRAINT fk_cintas_tecnologia REFERENCES tecnologias_lto(nombre)
                        ON UPDATE CASCADE ON DELETE RESTRICT,
    es_semanal          BOOLEAN DEFAULT FALSE,
    dia_semana          VARCHAR(20),
    contador_usos       INTEGER DEFAULT 0,
    fecha_primer_uso    TIMESTAMP,
    ultima_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    es_llenable         BOOLEAN DEFAULT TRUE
);

CREATE TABLE movimientos (
    id                  BIGSERIAL PRIMARY KEY,
    barcode             VARCHAR(50) REFERENCES cintas(barcode),
    tipo_movimiento     VARCHAR(20) NOT NULL
                        CONSTRAINT ck_movimientos_tipo CHECK (tipo_movimiento IN ('ALTA', 'RELACION', 'REINICIO', 'MONTAJE', 'DESMONTAJE',
                                                   'CAMBIO_UBICACION', 'MARCA_SEMANAL', 'DESMARCA_SEMANAL', 'RETIRO',
                                                   'MOVIMIENTO_FISICO')),  -- MOVIMIENTO_FISICO: tipo histórico de versiones anteriores
    fecha               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    observacion         TEXT,
    comprobante_digital TEXT,
    usuario             VARCHAR(50) CONSTRAINT fk_movimientos_usuario REFERENCES usuarios(username)
                        ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- La auditoría guarda el nombre de usuario como texto, sin clave foránea, a propósito:
-- también registra intentos de inicio de sesión con nombres de usuario inexistentes.
CREATE TABLE auditoria (
    id          BIGSERIAL PRIMARY KEY,
    fecha       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario     VARCHAR(50),
    tipo_evento VARCHAR(40),
    detalle     TEXT,
    ip          VARCHAR(45)
);

CREATE TABLE usuario_cliente (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, cliente_id)
);

CREATE TABLE auditoria_inventario (
    id                    BIGSERIAL PRIMARY KEY,
    fecha                 TIMESTAMP,
    usuario               VARCHAR(50),
    total_analizadas      INTEGER,
    coincidencias         INTEGER,
    total_inconsistencias INTEGER
);

CREATE TABLE auditoria_inventario_detalle (
    id               BIGSERIAL PRIMARY KEY,
    auditoria_id     BIGINT REFERENCES auditoria_inventario(id) ON DELETE CASCADE,
    barcode          VARCHAR(20),
    tipo             VARCHAR(30),
    descripcion      TEXT,
    ubicacion_bd     VARCHAR(30),
    ubicacion_fisica VARCHAR(30)
);

-- 3) Índices
CREATE INDEX idx_cintas_cliente        ON cintas(cliente_id);
CREATE INDEX idx_movimientos_barcode   ON movimientos(barcode);
CREATE INDEX idx_auditoria_fecha       ON auditoria(fecha);
CREATE INDEX idx_aud_inv_detalle_aud   ON auditoria_inventario_detalle(auditoria_id);

-- Un mismo Volume ID no puede estar vinculado a dos cintas (PENDIENTE = sin vincular)
CREATE UNIQUE INDEX ux_cintas_volume_id ON cintas(volume_id)
    WHERE volume_id IS NOT NULL AND volume_id <> 'PENDIENTE';

-- 4) Registros inmutables: auditoría, conciliaciones y movimientos no se modifican ni se eliminan
CREATE OR REPLACE FUNCTION fn_registro_inmutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de la tabla % no pueden modificarse ni eliminarse (registro inmutable).', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_auditoria_inmutable BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();
CREATE TRIGGER trg_auditoria_inventario_inmutable BEFORE UPDATE OR DELETE ON auditoria_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();
CREATE TRIGGER trg_auditoria_inventario_detalle_inmutable BEFORE UPDATE OR DELETE ON auditoria_inventario_detalle
    FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();
CREATE TRIGGER trg_movimientos_inmutable BEFORE UPDATE OR DELETE ON movimientos
    FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();

-- 5) Datos iniciales (semilla)
INSERT INTO roles (codigo, nombre, descripcion) VALUES
('ADMIN',    'Administrador', 'Acceso total: gestión de usuarios, configuración y auditoría.'),
('OPERADOR', 'Operador',      'Operación de cintas y clientes; sin acceso a administración.');

INSERT INTO tecnologias_lto (nombre, generacion, capacidad_gb) VALUES
('LTO-6', 6, 2500),
('LTO-7', 7, 6000),
('LTO-8', 8, 12000),
('LTO-9', 9, 18000);

-- Usuario técnico para operaciones internas del sistema (no puede iniciar sesión)
INSERT INTO usuarios (username, password_hash, nombre_completo, rol_id, activo)
VALUES ('sistema', 'SIN-ACCESO', 'Usuario técnico del sistema',
        (SELECT id FROM roles WHERE codigo = 'OPERADOR'), FALSE);
