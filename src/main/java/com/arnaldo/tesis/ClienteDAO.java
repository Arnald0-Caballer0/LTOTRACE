package com.arnaldo.tesis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteDAO extends JpaRepository<Cliente, Long> {
    // Spring Data JPA implementa todos los métodos básicos automáticamente.
    // Más adelante podemos agregar aquí búsquedas personalizadas si las necesitas.
}