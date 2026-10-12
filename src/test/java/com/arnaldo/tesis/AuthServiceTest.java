package com.arnaldo.tesis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * CP-04 — Bloqueo de autenticación por seguridad.
 * Verifica la política: 3 intentos fallidos -> bloqueo temporal de 10 min;
 * reincidencia tras el bloqueo -> cuenta deshabilitada.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CP-04 · AuthService: bloqueo por intentos fallidos")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setUsername("operador");
        usuario.setPasswordHash(encoder.encode("Correcta123"));
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        lenient().when(usuarioRepository.findByUsername("operador")).thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("Contraseña correcta devuelve EXITO y reinicia el contador")
    void loginCorrecto_devuelveExito() {
        ResultadoLogin r = authService.intentarLogin("operador", "Correcta123");

        assertThat(r.getEstado()).isEqualTo(ResultadoLogin.Estado.EXITO);
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
    }

    @Test
    @DisplayName("Tres intentos fallidos bloquean temporalmente la cuenta")
    void tresIntentosFallidos_bloqueaTemporalmente() {
        authService.intentarLogin("operador", "mala");
        authService.intentarLogin("operador", "mala");
        ResultadoLogin r = authService.intentarLogin("operador", "mala");

        assertThat(r.getEstado()).isEqualTo(ResultadoLogin.Estado.BLOQUEADO_TEMPORAL);
        assertThat(usuario.getBloqueadoHasta()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Durante el bloqueo, incluso la contraseña correcta es rechazada")
    void duranteBloqueo_rechazaAunConPasswordCorrecta() {
        authService.intentarLogin("operador", "mala");
        authService.intentarLogin("operador", "mala");
        authService.intentarLogin("operador", "mala"); // queda bloqueada

        ResultadoLogin r = authService.intentarLogin("operador", "Correcta123");

        assertThat(r.getEstado()).isEqualTo(ResultadoLogin.Estado.BLOQUEADO_TEMPORAL);
    }

    @Test
    @DisplayName("Reincidencia tras el bloqueo deshabilita la cuenta")
    void reincidencia_deshabilitaCuenta() {
        // Primer ciclo -> bloqueo temporal
        authService.intentarLogin("operador", "mala");
        authService.intentarLogin("operador", "mala");
        authService.intentarLogin("operador", "mala");

        // Simulamos que el bloqueo de 10 min ya venció (como indica el CP-04)
        usuario.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));

        // Segundo ciclo de 3 fallos -> deshabilita
        authService.intentarLogin("operador", "mala");
        authService.intentarLogin("operador", "mala");
        ResultadoLogin r = authService.intentarLogin("operador", "mala");

        assertThat(r.getEstado()).isEqualTo(ResultadoLogin.Estado.DESHABILITADO);
        assertThat(usuario.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Usuario inexistente devuelve credenciales inválidas (no revela si existe)")
    void usuarioInexistente_credencialesInvalidas() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        ResultadoLogin r = authService.intentarLogin("fantasma", "loquesea");

        assertThat(r.getEstado()).isEqualTo(ResultadoLogin.Estado.CREDENCIALES_INVALIDAS);
    }
}
