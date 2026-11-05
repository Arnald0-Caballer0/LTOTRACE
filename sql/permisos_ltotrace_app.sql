-- =====================================================================
-- LTOTRACE · Privilegios del usuario de la aplicación (mínimo privilegio)
-- Las tablas pertenecen al administrador del motor (postgres); la aplicación
-- se conecta con ltotrace_app, que solo puede leer y escribir datos, no
-- modificar la estructura, y no puede modificar ni eliminar los registros
-- de auditoría, conciliación y movimientos.
-- Ejecutar con postgres, después del script de creación:
--   sudo -u postgres psql -d atopa -f permisos_ltotrace_app.sql
-- =====================================================================
GRANT CONNECT ON DATABASE atopa TO ltotrace_app;
GRANT USAGE ON SCHEMA public TO ltotrace_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ltotrace_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ltotrace_app;

-- Registros inmutables: solo lectura e inserción
REVOKE UPDATE, DELETE, TRUNCATE
    ON auditoria, auditoria_inventario, auditoria_inventario_detalle, movimientos
  FROM ltotrace_app;
