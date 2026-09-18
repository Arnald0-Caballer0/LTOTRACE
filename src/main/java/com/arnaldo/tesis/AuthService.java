package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de autenticación con bloqueo por intentos fallidos:
 *  - 3 intentos fallidos  -> bloqueo temporal de 10 minutos.
 *  - si tras el bloqueo vuelve a fallar 3 veces -> cuenta deshabilitada
 *    (solo un administrador puede reactivarla).
 */
@Service
public class AuthService {

    private static final int MAX_INTENTOS = 3;
    private static final int MINUTOS_BLOQUEO = 10;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Procesa un intento de login y devuelve su resultado (estado).
     * Actualiza los contadores de intentos y el bloqueo según corresponda.
     */
    public ResultadoLogin intentarLogin(String username, String passwordPlano) {
        if (username == null || passwordPlano == null) {
            return ResultadoLogin.credencialesInvalidas();
        }

        Optional<Usuario> opt = usuarioRepository.findByUsername(username.trim());
        if (opt.isEmpty()) {
            // No revelamos si el usuario existe o no.
            return ResultadoLogin.credencialesInvalidas();
        }
        Usuario u = opt.get();

        // 1) Cuenta deshabilitada (requiere reactivación de un administrador)
        if (u.getActivo() == null || !u.getActivo()) {
            return ResultadoLogin.deshabilitado();
        }

        // 2) Bloqueo temporal vigente
        if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            long segs = Duration.between(LocalDateTime.now(), u.getBloqueadoHasta()).getSeconds();
            long min = Math.max(1, (segs + 59) / 60);
            return ResultadoLogin.bloqueadoTemporal(min);
        }

        // 3) Contraseña correcta -> entra y limpia contadores
        if (encoder.matches(passwordPlano, u.getPasswordHash())) {
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
            usuarioRepository.save(u);
            return ResultadoLogin.exito(u);
        }

        // 4) Contraseña incorrecta -> suma intento
        int intentos = (u.getIntentosFallidos() == null ? 0 : u.getIntentosFallidos()) + 1;
        u.setIntentosFallidos(intentos);

        if (intentos >= MAX_INTENTOS) {
            if (u.getBloqueadoHasta() == null) {
                // Primer ciclo: bloqueo temporal de 10 minutos
                u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
                u.setIntentosFallidos(0);
                usuarioRepository.save(u);
                return ResultadoLogin.bloqueadoTemporal(MINUTOS_BLOQUEO);
            } else {
                // Segundo ciclo: deshabilitar la cuenta y limpiar contadores
                u.setActivo(false);
                u.setIntentosFallidos(0);
                u.setBloqueadoHasta(null);
                usuarioRepository.save(u);
                return ResultadoLogin.deshabilitado();
            }
        }

        usuarioRepository.save(u);
        return ResultadoLogin.credencialesInvalidas();
    }

    /** Utilidad para generar un hash BCrypt (al crear usuarios nuevos). */
    public String hashear(String passwordPlano) {
        return encoder.encode(passwordPlano);
    }
}
