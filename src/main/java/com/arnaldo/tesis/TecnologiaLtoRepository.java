package com.arnaldo.tesis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TecnologiaLtoRepository extends JpaRepository<TecnologiaLto, Long> {
    List<TecnologiaLto> findAllByOrderByGeneracionAsc();
}
