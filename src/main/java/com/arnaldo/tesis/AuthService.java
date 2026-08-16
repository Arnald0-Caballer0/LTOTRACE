package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio de autenticación. Verifica el usuario y la contraseña
 * comparando contra el hash BCrypt guardado en la base de datos.
 */
@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // BCrypt: mismo algoritmo con el que se generó el hash del admin
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Devuelve el Usuario si las credenciales son válidas, o null si no lo son.
     */
    public Usuario autenticar(String username, String passwordPlano) {
        if (username == null || passwordPlano == null) {
            return null;
        }

        Optional<Usuario> opt = usuarioRepository.findByUsernameAndActivoTrue(username.trim());
        if (opt.isEmpty()) {
            return null;
        }

        Usuario usuario = opt.get();
        if (encoder.matches(passwordPlano, usuario.getPasswordHash())) {
            return usuario;
        }
        return null;
    }

    /**
     * Utilidad para generar un hash BCrypt (útil al crear usuarios nuevos).
     */
    public String hashear(String passwordPlano) {
        return encoder.encode(passwordPlano);
    }
}
