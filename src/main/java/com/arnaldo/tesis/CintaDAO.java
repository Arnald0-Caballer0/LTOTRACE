package com.arnaldo.tesis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CintaDAO {

    public void registrarCinta(String barcode, String volumeId, int clienteId) {
        String sql = "INSERT INTO cintas (barcode, volume_id, cliente_id, ubicacion) VALUES (?, ?, ?, 'ARMARIO')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            pstmt.setString(2, volumeId);
            pstmt.setInt(3, clienteId);

            pstmt.executeUpdate();
            System.out.println("Cinta " + barcode + " registrada con éxito (VolID: " + volumeId + ")");

        } catch (SQLException e) {
            System.err.println("Error al registrar la cinta: " + e.getMessage());
        }
    }
    // Agrega este método a tu clase CintaDAO.java
    public void actualizarVolumeId(String barcode, String volumeId) {
        String sql = "UPDATE cintas SET volume_id = ?, ultima_modificacion = CURRENT_TIMESTAMP WHERE barcode = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, volumeId);
            pstmt.setString(2, barcode);

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("PostgreSQL actualizado: " + barcode + " ahora se llama " + volumeId);
            } else {
                System.err.println("No se encontró la cinta con barcode: " + barcode);
            }

        } catch (SQLException e) {
            System.err.println("Error al actualizar en Postgres: " + e.getMessage());
        }
    }
}
