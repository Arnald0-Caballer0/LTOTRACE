package com.arnaldo.tesis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    // Últimos 500 eventos, más recientes primero
    List<Auditoria> findTop500ByOrderByFechaDesc();
}
