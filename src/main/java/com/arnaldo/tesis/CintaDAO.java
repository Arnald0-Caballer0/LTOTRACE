package com.arnaldo.tesis;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CintaDAO {

    // ==========================================================
    // NÚCLEO DE TRAZABILIDAD: registro de movimientos
    // ==========================================================

    /** Inserta un movimiento usando una conexión YA ABIERTA (dentro de una transacción). */
    private void registrarMovimiento(Connection conn, String barcode, String tipoMovimiento,
                                     String observacion, String comprobanteDigital) throws SQLException {
        String sql = "INSERT INTO movimientos (barcode, tipo_movimiento, observacion, comprobante_digital, usuario) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            pstmt.setString(2, tipoMovimiento);
            pstmt.setString(3, observacion);
            pstmt.setString(4, comprobanteDigital);
            pstmt.setString(5, UsuarioActual.get()); // QUIÉN hizo el movimiento
            pstmt.executeUpdate();
        }
    }

    /** Registra un movimiento de forma independiente (abre su propia conexión). */
    public void registrarMovimiento(String barcode, String tipoMovimiento, String observacion) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            registrarMovimiento(conn, barcode, tipoMovimiento, observacion, null);
            System.out.println("Movimiento registrado: " + barcode + " -> " + tipoMovimiento);
        } catch (SQLException e) {
            System.err.println("Error al registrar movimiento: " + e.getMessage());
        }
    }

    /** Lee el historial de una cinta (más reciente primero). */
    public List<Movimiento> listarMovimientos(String barcode) {
        List<Movimiento> lista = new ArrayList<>();
        String sql = "SELECT id, barcode, tipo_movimiento, fecha, observacion, comprobante_digital, usuario " +
                "FROM movimientos WHERE barcode = ? ORDER BY fecha DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                lista.add(new Movimiento(
                        rs.getLong("id"),
                        rs.getString("barcode"),
                        rs.getString("tipo_movimiento"),
                        rs.getTimestamp("fecha") != null ? rs.getTimestamp("fecha").toLocalDateTime() : null,
                        rs.getString("observacion"),
                        rs.getString("comprobante_digital"),
                        rs.getString("usuario")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar movimientos: " + e.getMessage());
        }
        return lista;
    }

    // Helpers de transacción -----------------------------------

    private void rollback(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ex) { System.err.println("Error en rollback: " + ex.getMessage()); }
        }
    }

    private void cerrar(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { System.err.println("Error al cerrar conexión: " + ex.getMessage()); }
        }
    }

    // ==========================================================
    // DESGASTE Y LLENADO: registrar un montaje (un "uso")
    // ==========================================================
    /**
     * Se llama cuando la cinta se CARGA en el drive (un uso real).
     * Sube el contador de usos, fija la fecha de primer uso si no existía,
     * y registra el movimiento MONTAJE, todo en una sola transacción.
     */
    public void registrarMontaje(String barcode) {
        String sql = "UPDATE cintas SET " +
                "contador_usos = COALESCE(contador_usos, 0) + 1, " +
                "fecha_primer_uso = COALESCE(fecha_primer_uso, CURRENT_TIMESTAMP), " +
                "ultima_modificacion = CURRENT_TIMESTAMP " +
                "WHERE barcode = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int filas;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, barcode);
                filas = pstmt.executeUpdate();
            }

            if (filas > 0) {
                registrarMovimiento(conn, barcode, "MONTAJE", "Cinta montada en el drive (uso registrado)", null);
                conn.commit();
                System.out.println("Montaje registrado para " + barcode);
            } else {
                rollback(conn);
            }
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al registrar montaje: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }

    // ==========================================================
    // 1. Registro inicial (cuando entra la cinta al armario)
    // ==========================================================
    public void registrarCinta(String barcode, String volumeId, int clienteId, String tecnologia) {
        String sql = "INSERT INTO cintas (barcode, volume_id, cliente_id, ubicacion, tecnologia) " +
                "VALUES (?, ?, ?, 'ARMARIO', ?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, barcode);
                pstmt.setString(2, volumeId);
                pstmt.setInt(3, clienteId);
                pstmt.setString(4, tecnologia);
                pstmt.executeUpdate();
            }

            registrarMovimiento(conn, barcode, "ALTA", "Cinta registrada en ARMARIO (" + tecnologia + ")", null);

            conn.commit();
            System.out.println("Cinta " + barcode + " registrada con éxito.");
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al registrar: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }

    // ==========================================================
    // 2. El "Matching": Actualiza el Volume ID obtenido del IBM i (CHKTAP)
    // ==========================================================
    public void actualizarVolumeId(String barcode, String volumeId) {
        String sql = "UPDATE cintas SET volume_id = ?, ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int filasAfectadas;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, volumeId);
                pstmt.setString(2, barcode);
                filasAfectadas = pstmt.executeUpdate();
            }

            if (filasAfectadas > 0) {
                registrarMovimiento(conn, barcode, "RELACION", "Volume ID relacionado: " + volumeId, null);
                conn.commit();
                System.out.println("PostgreSQL actualizado: " + barcode + " -> " + volumeId);
            } else {
                rollback(conn);
                System.err.println("No se encontró el barcode: " + barcode);
            }
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al actualizar en Postgres: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }

    // ==========================================================
    // 3. Listados para la Interfaz Web - SOLO LECTURA
    // ==========================================================
    public List<Cinta> filtrarCintas(Integer clienteId, String tecnologia) {
        List<Cinta> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente, " +
                        "c.contador_usos, cl.promedio_gb_backup, cl.dias_entre_backups " +
                        "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id WHERE 1=1"
        );

        if (clienteId != null) sql.append(" AND c.cliente_id = ?");
        if (tecnologia != null) sql.append(" AND c.tecnologia = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int i = 1;
            if (clienteId != null) pstmt.setInt(i++, clienteId);
            if (tecnologia != null) pstmt.setString(i++, tecnologia);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Cinta cinta = new Cinta(
                        rs.getString("barcode"), rs.getString("volume_id"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getString("cliente"), rs.getString("tecnologia")
                );
                // Datos para desgaste y llenado
                cinta.setContadorUsos(rs.getInt("contador_usos"));
                cinta.setPromedioGbBackup(rs.getInt("promedio_gb_backup"));
                cinta.setDiasEntreBackups(rs.getInt("dias_entre_backups"));
                lista.add(cinta);
            }
        } catch (SQLException e) {
            System.err.println("Error en filtro: " + e.getMessage());
        }
        return lista;
    }


    public List<Cinta> listarTodas() {
        List<Cinta> lista = new ArrayList<>();
        String sql = "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente " +
                "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(new Cinta(
                        rs.getString("barcode"), rs.getString("volume_id"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getString("cliente"), rs.getString("tecnologia")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar cintas: " + e.getMessage());
        }
        return lista;
    }

    public List<Cinta> filtrarPorCliente(int clienteId) {
        List<Cinta> lista = new ArrayList<>();
        String sql = "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente " +
                "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id WHERE c.cliente_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clienteId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                lista.add(new Cinta(
                        rs.getString("barcode"), rs.getString("volume_id"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getString("cliente"), rs.getString("tecnologia")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error en filtrarPorCliente: " + e.getMessage());
        }
        return lista;
    }

    // ==========================================================
    // 4. Reiniciar la cinta (Vuelve a estado PENDIENTE)
    // ==========================================================
    public void reiniciarCinta(String barcode) {
        String sql = "UPDATE cintas SET volume_id = 'PENDIENTE', ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int filas;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, barcode);
                filas = pstmt.executeUpdate();
            }

            if (filas > 0) {
                registrarMovimiento(conn, barcode, "REINICIO", "Cinta reiniciada, lista para nuevo matching", null);
                conn.commit();
                System.out.println("Cinta " + barcode + " reiniciada. Lista para nuevo matching.");
            } else {
                rollback(conn);
            }
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al reiniciar cinta: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }

    public List<Cinta> listarRecientes(Integer clienteId, String tecnologia) {
        List<Cinta> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente " +
                        "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id WHERE 1=1 "
        );

        if (clienteId != null) sql.append(" AND c.cliente_id = ?");
        if (tecnologia != null) sql.append(" AND c.tecnologia = ?");
        sql.append(" ORDER BY c.ultima_modificacion DESC LIMIT 5");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (clienteId != null) pstmt.setInt(i++, clienteId);
            if (tecnologia != null) pstmt.setString(i++, tecnologia);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                lista.add(new Cinta(rs.getString("barcode"), rs.getString("volume_id"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getString("cliente"), rs.getString("tecnologia")));
            }
        } catch (SQLException e) {
            System.err.println("Error en recientes: " + e.getMessage());
        }
        return lista;
    }

    // ==========================================================
    // 5. Marcar/desmarcar semanal (con día específico)
    // ==========================================================
    public void asignarDiaSemanal(String barcode, String dia) {
        String sql = "UPDATE cintas SET es_semanal = TRUE, dia_semana = ? WHERE barcode = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int filas;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, dia);
                pstmt.setString(2, barcode);
                filas = pstmt.executeUpdate();
            }
            if (filas > 0) {
                registrarMovimiento(conn, barcode, "MARCA_SEMANAL", "Marcada como semanal (" + dia + ")", null);
                conn.commit();
            } else {
                rollback(conn);
            }
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al asignar día: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }

    public void quitarDiaSemanal(String barcode) {
        String sql = "UPDATE cintas SET es_semanal = FALSE, dia_semana = NULL WHERE barcode = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int filas;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, barcode);
                filas = pstmt.executeUpdate();
            }
            if (filas > 0) {
                registrarMovimiento(conn, barcode, "DESMARCA_SEMANAL", "Quitada de semanales", null);
                conn.commit();
            } else {
                rollback(conn);
            }
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al quitar día: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }

    public void actualizarEstadoSemanal(String barcode, boolean esSemanal) {
        String sql = "UPDATE cintas SET es_semanal = ? WHERE barcode = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int filas;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, esSemanal);
                pstmt.setString(2, barcode);
                filas = pstmt.executeUpdate();
            }
            if (filas > 0) {
                registrarMovimiento(conn, barcode,
                        esSemanal ? "MARCA_SEMANAL" : "DESMARCA_SEMANAL",
                        esSemanal ? "Marcada como semanal" : "Quitada de semanales", null);
                conn.commit();
            } else {
                rollback(conn);
            }
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al actualizar estado semanal: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }

    public List<Cinta> listarSemanalesManual(Integer clienteId, String tecnologia) {
        List<Cinta> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente, c.dia_semana " +
                        "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id WHERE c.es_semanal = TRUE"
        );

        if (clienteId != null) sql.append(" AND c.cliente_id = ?");
        if (tecnologia != null && !tecnologia.isEmpty()) sql.append(" AND c.tecnologia = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (clienteId != null) pstmt.setInt(paramIndex++, clienteId);
            if (tecnologia != null && !tecnologia.isEmpty()) pstmt.setString(paramIndex++, tecnologia);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Cinta cinta = new Cinta(
                        rs.getString("barcode"), rs.getString("volume_id"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getString("cliente"), rs.getString("tecnologia")
                );
                cinta.setDiaSemana(rs.getString("dia_semana"));
                lista.add(cinta);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar semanales: " + e.getMessage());
        }
        return lista;
    }

    // ==========================================================
    // 6. Retiro de múltiples cintas (guardando el comprobante)
    // ==========================================================
    public void retirarCintas(List<String> barcodes) {
        retirarCintas(barcodes, null);
    }

    public void retirarCintas(List<String> barcodes, String comprobanteBase64) {
        if (barcodes == null || barcodes.isEmpty()) return;

        StringBuilder sql = new StringBuilder(
                "UPDATE cintas SET ubicacion = 'RETIRADO', ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode IN (");
        for (int i = 0; i < barcodes.size(); i++) {
            sql.append("?");
            if (i < barcodes.size() - 1) sql.append(",");
        }
        sql.append(")");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int filasAfectadas;
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < barcodes.size(); i++) {
                    pstmt.setString(i + 1, barcodes.get(i));
                }
                filasAfectadas = pstmt.executeUpdate();
            }

            // Un movimiento RETIRO por cinta, guardando el comprobante (PDF en Base64)
            for (String barcode : barcodes) {
                registrarMovimiento(conn, barcode, "RETIRO", "Cinta retirada del inventario", comprobanteBase64);
            }

            conn.commit();
            System.out.println("Éxito: Se han retirado " + filasAfectadas + " cintas en la base de datos.");
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al procesar el retiro masivo: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }


    // ==========================================================
    // 7. Cambiar la ubicación de una sola cinta (Inventario)
    // ==========================================================
    public void actualizarUbicacion(String barcode, String nuevaUbicacion) {
        String sql = "UPDATE cintas SET ubicacion = ?, ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int filas;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nuevaUbicacion);
                pstmt.setString(2, barcode);
                filas = pstmt.executeUpdate();
            }
            if (filas > 0) {
                registrarMovimiento(conn, barcode, "CAMBIO_UBICACION", "Nueva ubicación: " + nuevaUbicacion, null);
                conn.commit();
            } else {
                rollback(conn);
            }
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al actualizar ubicación: " + e.getMessage());
        } finally {
            cerrar(conn);
        }
    }

    // Trae las cintas indicadas (para armar el comprobante de retiro)
    public List<Cinta> buscarPorBarcodes(List<String> barcodes) {
        List<Cinta> lista = new ArrayList<>();
        if (barcodes == null || barcodes.isEmpty()) return lista;

        StringBuilder sql = new StringBuilder(
                "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente " +
                        "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id WHERE c.barcode IN (");
        for (int i = 0; i < barcodes.size(); i++) {
            sql.append("?");
            if (i < barcodes.size() - 1) sql.append(",");
        }
        sql.append(") ORDER BY c.barcode");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < barcodes.size(); i++) {
                pstmt.setString(i + 1, barcodes.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                lista.add(new Cinta(
                        rs.getString("barcode"), rs.getString("volume_id"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getString("cliente"), rs.getString("tecnologia")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error en buscarPorBarcodes: " + e.getMessage());
        }
        return lista;
    }

    // Devuelve el comprobante (PDF en Base64) guardado en un movimiento, o null si no tiene
    public String obtenerComprobante(Long movimientoId) {
        String sql = "SELECT comprobante_digital FROM movimientos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, movimientoId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("comprobante_digital");
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener comprobante: " + e.getMessage());
        }
        return null;
    }

    // Trae TODAS las cintas con TODOS sus campos (para el módulo Cintas > Ver cintas)
    public List<java.util.Map<String, Object>> listarTodasCompleto() {
        List<java.util.Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT c.barcode, c.volume_id, c.tecnologia, c.ubicacion, c.estado, " +
                "c.contador_usos, c.fecha_primer_uso, c.es_semanal, c.dia_semana, " +
                "c.ultima_modificacion, cl.nombre AS cliente " +
                "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id ORDER BY c.barcode";

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        java.time.format.DateTimeFormatter fmtFecha = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("barcode", rs.getString("barcode"));
                m.put("volumeId", rs.getString("volume_id"));
                m.put("tecnologia", rs.getString("tecnologia"));
                m.put("ubicacion", rs.getString("ubicacion"));
                m.put("estado", rs.getString("estado"));
                m.put("cliente", rs.getString("cliente"));
                m.put("usos", rs.getInt("contador_usos"));
                m.put("esSemanal", rs.getBoolean("es_semanal"));
                m.put("diaSemana", rs.getString("dia_semana"));

                java.sql.Timestamp fpu = rs.getTimestamp("fecha_primer_uso");
                m.put("fechaPrimerUso", fpu != null ? fpu.toLocalDateTime().format(fmtFecha) : "");
                java.sql.Timestamp um = rs.getTimestamp("ultima_modificacion");
                m.put("ultimaModificacion", um != null ? um.toLocalDateTime().format(fmt) : "");

                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error en listarTodasCompleto: " + e.getMessage());
        }
        return lista;
    }
    // Crea un lote de cintas en una sola transacción. Omite las que ya existan.
    public java.util.Map<String, Object> crearCintasLote(java.util.List<String> barcodes, int clienteId, String tecnologia) {
        java.util.List<String> creadas = new ArrayList<>();
        java.util.List<String> omitidas = new ArrayList<>();

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String check = "SELECT 1 FROM cintas WHERE barcode = ?";
            String insert = "INSERT INTO cintas (barcode, volume_id, cliente_id, ubicacion, tecnologia) " +
                    "VALUES (?, 'PENDIENTE', ?, 'ARMARIO', ?)";

            for (String bc : barcodes) {
                boolean existe;
                try (PreparedStatement ps = conn.prepareStatement(check)) {
                    ps.setString(1, bc);
                    existe = ps.executeQuery().next();
                }
                if (existe) { omitidas.add(bc); continue; }

                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, bc);
                    ps.setInt(2, clienteId);
                    ps.setString(3, tecnologia);
                    ps.executeUpdate();
                }
                registrarMovimiento(conn, bc, "ALTA", "Cinta creada por lote (" + tecnologia + ")", null);
                creadas.add(bc);
            }

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            System.err.println("Error al crear lote de cintas: " + e.getMessage());
        } finally {
            cerrar(conn);
        }

        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("creadas", creadas);
        res.put("omitidas", omitidas);
        return res;
    }


}
