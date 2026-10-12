package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provee conexiones JDBC reutilizando el pool de conexiones configurado por
 * Spring Boot (spring.datasource.* en application.properties). De este modo las
 * credenciales NO están embebidas en el código fuente y se evita duplicar la
 * configuración de la base de datos.
 *
 * Como el DAO usa esta clase de forma estática, se emplea un "puente": Spring
 * inyecta el DataSource una vez al arrancar y se guarda en una referencia estática.
 */
@Component
public class DatabaseConnection {

    private static DataSource dataSource;

    @Autowired
    public void setDataSource(DataSource ds) {
        DatabaseConnection.dataSource = ds;
    }

    public static Connection getConnection() {
        try {
            if (dataSource == null) {
                System.err.println("DataSource aún no disponible.");
                return null;
            }
            return dataSource.getConnection();
        } catch (SQLException e) {
            System.err.println("Error al obtener conexión del pool: " + e.getMessage());
            return null;
        }
    }
}
