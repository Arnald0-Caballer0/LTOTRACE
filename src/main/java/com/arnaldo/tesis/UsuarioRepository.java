package com.arnaldo.tesis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario activo por su nombre de usuario (para el login)
    Optional<Usuario> findByUsernameAndActivoTrue(String username);
}
