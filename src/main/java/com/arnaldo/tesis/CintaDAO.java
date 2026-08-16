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

    /**
     * Inserta un movimiento usando una conexión YA ABIERTA.
     * Se usa dentro de una transacción, junto con el cambio en "cintas",
     * para que ambos se confirmen (commit) o se deshagan (rollback) juntos.
     */
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

    /**
     * Registra un movimiento de forma independiente (abre su propia conexión).
     * Útil para eventos que NO modifican la tabla "cintas" en este DAO,
     * como el movimiento físico del robot TS4300.
     */
    public void registrarMovimiento(String barcode, String tipoMovimiento, String observacion) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            registrarMovimiento(conn, barcode, tipoMovimiento, observacion, null);
            System.out.println("Movimiento registrado: " + barcode + " -> " + tipoMovimiento);
        } catch (SQLException e) {
            System.err.println("Error al registrar movimiento: " + e.getMessage());
        }
    }

    /** Lee el historial de una cinta (más reciente primero). Para la vista de trazabilidad. */
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
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error en rollback: " + ex.getMessage());
            }
        }
    }

    private void cerrar(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ex) {
                System.err.println("Error al cerrar conexión: " + ex.getMessage());
            }
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
            conn.setAutoCommit(false); // Inicia transacción

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, barcode);
                pstmt.setString(2, volumeId);
                pstmt.setInt(3, clienteId);
                pstmt.setString(4, tecnologia);
                pstmt.executeUpdate();
            }

            // La cinta ya existe dentro de la transacción, así que la FK del movimiento es válida
            registrarMovimiento(conn, barcode, "ALTA",
                    "Cinta registrada en ARMARIO (" + tecnologia + ")", null);

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
                registrarMovimiento(conn, barcode, "RELACION",
                        "Volume ID relacionado: " + volumeId, null);
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
    // 3. Listado para la Interfaz Web (Atopa Dashboard) - SOLO LECTURA
    // ==========================================================
    public List<Cinta> filtrarCintas(Integer clienteId, String tecnologia) {
        List<Cinta> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente " +
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
                lista.add(new Cinta(
                        rs.getString("barcode"), rs.getString("volume_id"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getString("cliente"), rs.getString("tecnologia")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error en filtro: " + e.getMessage());
        }
        return lista;
    }

    // 1. Método para listar todas (Actualizado con Tecnología) - SOLO LECTURA
    public List<Cinta> listarTodas() {
        List<Cinta> lista = new ArrayList<>();
        String sql = "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente " +
                "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(new Cinta(
                        rs.getString("barcode"),
                        rs.getString("volume_id"),
                        rs.getString("ubicacion"),
                        rs.getString("estado"),
                        rs.getString("cliente"),
                        rs.getString("tecnologia")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar cintas: " + e.getMessage());
        }
        return lista;
    }

    // 2. filtrarPorCliente - SOLO LECTURA
    public List<Cinta> filtrarPorCliente(int clienteId) {
        List<Cinta> lista = new ArrayList<>();
        String sql = "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente " +
                "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id " +
                "WHERE c.cliente_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clienteId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                lista.add(new Cinta(
                        rs.getString("barcode"),
                        rs.getString("volume_id"),
                        rs.getString("ubicacion"),
                        rs.getString("estado"),
                        rs.getString("cliente"),
                        rs.getString("tecnologia")
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
                registrarMovimiento(conn, barcode, "REINICIO",
                        "Cinta reiniciada, lista para nuevo matching", null);
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

    // Recientes - SOLO LECTURA
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
                registrarMovimiento(conn, barcode, "MARCA_SEMANAL",
                        "Marcada como semanal (" + dia + ")", null);
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
                registrarMovimiento(conn, barcode, "DESMARCA_SEMANAL",
                        "Quitada de semanales", null);
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

    // Compatibilidad: marcar/desmarcar semanal sin día (mantiene tu firma anterior)
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

    // Listar semanales - SOLO LECTURA
    public List<Cinta> listarSemanalesManual(Integer clienteId, String tecnologia) {
        List<Cinta> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente, c.dia_semana " +
                        "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id " +
                        "WHERE c.es_semanal = TRUE"
        );

        if (clienteId != null) {
            sql.append(" AND c.cliente_id = ?");
        }
        if (tecnologia != null && !tecnologia.isEmpty()) {
            sql.append(" AND c.tecnologia = ?");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (clienteId != null) {
                pstmt.setInt(paramIndex++, clienteId);
            }
            if (tecnologia != null && !tecnologia.isEmpty()) {
                pstmt.setString(paramIndex++, tecnologia);
            }

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
    // 6. Retiro de múltiples cintas (en una sola transacción)
    // ==========================================================
    public void retirarCintas(List<String> barcodes) {
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

            // Un movimiento de RETIRO por cada cinta afectada
            for (String barcode : barcodes) {
                registrarMovimiento(conn, barcode, "RETIRO",
                        "Cinta retirada del inventario", null);
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
                registrarMovimiento(conn, barcode, "CAMBIO_UBICACION",
                        "Nueva ubicación: " + nuevaUbicacion, null);
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
}
