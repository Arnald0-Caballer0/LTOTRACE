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

    // 1. Registro inicial (cuando entra la cinta al armario)
    public void registrarCinta(String barcode, String volumeId, int clienteId, String tecnologia) {
        String sql = "INSERT INTO cintas (barcode, volume_id, cliente_id, ubicacion, tecnologia) VALUES (?, ?, ?, 'ARMARIO', ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            pstmt.setString(2, volumeId);
            pstmt.setInt(3, clienteId);
            pstmt.setString(4, tecnologia);

            pstmt.executeUpdate();
            System.out.println("Cinta " + barcode + " registrada con éxito.");

        } catch (SQLException e) {
            System.err.println("Error al registrar: " + e.getMessage());
        }
    }

    // 2. El "Matching": Actualiza el Volume ID obtenido del IBM i (CHKTAP)
    public void actualizarVolumeId(String barcode, String volumeId) {
        String sql = "UPDATE cintas SET volume_id = ?, ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, volumeId);
            pstmt.setString(2, barcode);

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("PostgreSQL actualizado: " + barcode + " -> " + volumeId);
            } else {
                System.err.println("No se encontró el barcode: " + barcode);
            }

        } catch (SQLException e) {
            System.err.println("Error al actualizar en Postgres: " + e.getMessage());
        }
    }

    // 3. Listado para la Interfaz Web (Atopa Dashboard)
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

    // 1. Método para filtrar por cliente (Para el selector de la derecha)
    // 1. Método para listar todas (Actualizado con Tecnología)
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
                        rs.getString("tecnologia") // Este es el 6to argumento que faltaba
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar cintas: " + e.getMessage());
        }
        return lista;
    }

    // 2. Tu método filtrarPorCliente (Corregido según tu captura de pantalla)
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
                        rs.getString("tecnologia") //Agregado para cumplir los 6 argumentos
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error en filtrarPorCliente: " + e.getMessage());
        }
        return lista;
    }

    // 2. Método para reiniciar la cinta (Vuelve a estado PENDIENTE)
    public void reiniciarCinta(String barcode) {
        // Ponemos el volume_id de nuevo en 'PENDIENTE' para permitir un nuevo matching
        String sql = "UPDATE cintas SET volume_id = 'PENDIENTE', ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            int filas = pstmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Cinta " + barcode + " reiniciada. Lista para nuevo matching.");
            }
        } catch (SQLException e) {
            System.err.println("Error al reiniciar cinta: " + e.getMessage());
        }
    }

    public List<Cinta> listarRecientes(Integer clienteId, String tecnologia) {
        List<Cinta> lista = new ArrayList<>();
        // SQL con LIMIT 5 y ordenado por los últimos cambios
        StringBuilder sql = new StringBuilder(
                "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente " +
                        "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id WHERE 1=1 "
        );

        if (clienteId != null) sql.append(" AND c.cliente_id = ?");
        if (tecnologia != null) sql.append(" AND c.tecnologia = ?");

        sql.append(" ORDER BY c.ultima_modificacion DESC LIMIT 5"); // El secreto de la semana

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
        } catch (SQLException e) { System.err.println("Error en recientes: " + e.getMessage()); }
        return lista;

    }

    // Para marcar o desmarcar una cinta como semanal
    public void actualizarEstadoSemanal(String barcode, boolean esSemanal) {
        String sql = "UPDATE cintas SET es_semanal = ? WHERE barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, esSemanal);
            pstmt.setString(2, barcode);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar estado semanal: " + e.getMessage());
        }
    }

    // Para listar solo las que tú seleccionaste
    public List<Cinta> listarSemanalesManual(Integer clienteId, String tecnologia) {
        List<Cinta> lista = new ArrayList<>();
        // 1. Agregamos c.dia_semana al SELECT
        StringBuilder sql = new StringBuilder(
                "SELECT c.barcode, c.volume_id, c.ubicacion, c.estado, c.tecnologia, cl.nombre as cliente, c.dia_semana " +
                        "FROM cintas c LEFT JOIN clientes cl ON c.cliente_id = cl.id " +
                        "WHERE c.es_semanal = TRUE"
        );

        if (clienteId != null) sql.append(" AND c.cliente_id = ?");
        if (tecnologia != null && !tecnologia.isEmpty()) sql.append(" AND c.tecnologia = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            // ... (seteos de los preapared statements) ...
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Cinta cinta = new Cinta(
                        rs.getString("barcode"), rs.getString("volume_id"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getString("cliente"), rs.getString("tecnologia")
                );
                // 2. Le inyectamos el día a la cinta
                cinta.setDiaSemana(rs.getString("dia_semana"));
                lista.add(cinta);
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return lista;
    }

    // ==========================================
    // NUEVO: Método para procesar el retiro de múltiples cintas
    // ==========================================
    public void retirarCintas(List<String> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) return;

        // Construimos un SQL dinámico con tantos "?" como cintas seleccionadas
        StringBuilder sql = new StringBuilder("UPDATE cintas SET ubicacion = 'RETIRADO', ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode IN (");
        for (int i = 0; i < barcodes.size(); i++) {
            sql.append("?");
            if (i < barcodes.size() - 1) sql.append(",");
        }
        sql.append(")");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            // Asignamos cada barcode a su respectivo "?"
            for (int i = 0; i < barcodes.size(); i++) {
                pstmt.setString(i + 1, barcodes.get(i));
            }

            int filasAfectadas = pstmt.executeUpdate();
            System.out.println("Éxito: Se han retirado " + filasAfectadas + " cintas en la base de datos.");

        } catch (SQLException e) {
            System.err.println("Error al procesar el retiro masivo: " + e.getMessage());
        }
    }

    // Método para cambiar la ubicación de una sola cinta desde el Inventario
    public void actualizarUbicacion(String barcode, String nuevaUbicacion) {
        String sql = "UPDATE cintas SET ubicacion = ?, ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nuevaUbicacion);
            pstmt.setString(2, barcode);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar ubicación: " + e.getMessage());
        }
    }

    // Método para asignar un día específico
    public void asignarDiaSemanal(String barcode, String dia) {
        String sql = "UPDATE cintas SET es_semanal = TRUE, dia_semana = ? WHERE barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dia);
            pstmt.setString(2, barcode);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Error al asignar día: " + e.getMessage()); }
    }

    // Método para vaciar la casilla
    public void quitarDiaSemanal(String barcode) {
        String sql = "UPDATE cintas SET es_semanal = FALSE, dia_semana = NULL WHERE barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Error al quitar día: " + e.getMessage()); }
    }

}