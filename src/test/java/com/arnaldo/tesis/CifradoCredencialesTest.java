package com.arnaldo.tesis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP-03 — Almacenamiento seguro de contraseñas y credenciales de conexión.
 * BCrypt para las contraseñas de usuario y AES para las credenciales del
 * IBM i y la TS4300. Ninguna credencial debe quedar en texto plano.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CP-03 · Cifrado de credenciales (BCrypt y AES)")
class CifradoCredencialesTest {

    @Mock
    private ConfiguracionRepository repo;

    @InjectMocks
    private ConfiguracionService configuracionService;

    @BeforeEach
    void setUp() {
        // La clave/salt normalmente vienen de application.properties; las fijamos para el test.
        ReflectionTestUtils.setField(configuracionService, "cryptoPassword", "clave-de-prueba-2026");
        ReflectionTestUtils.setField(configuracionService, "cryptoSalt", "0458349c980526e7");
    }

    // ---------- BCrypt (contraseñas de usuario) ----------

    @Test
    @DisplayName("BCrypt: la misma contraseña genera hashes distintos (sal aleatoria)")
    void bcrypt_mismaPasswordGeneraHashesDistintos() {
        AuthService auth = new AuthService();

        String h1 = auth.hashear("Secreta123");
        String h2 = auth.hashear("Secreta123");

        assertThat(h1).isNotEqualTo(h2);          // sal aleatoria -> resúmenes distintos
        assertThat(h1).startsWith("$2a$").hasSize(60);
        assertThat(new BCryptPasswordEncoder().matches("Secreta123", h1)).isTrue();
    }

    // ---------- AES (credenciales de conexión) ----------

    @Test
    @DisplayName("AES: el secreto se guarda cifrado y se recupera en claro (round-trip)")
    void aes_secretoSeGuardaCifradoYSeRecupera() {
        String plano = "***ELIMINADO***";

        ArgumentCaptor<Configuracion> captor = ArgumentCaptor.forClass(Configuracion.class);
        configuracionService.setSecreto("ibmi.password", plano);
        verify(repo).save(captor.capture());
        String guardado = captor.getValue().getValor();

        // 1) lo almacenado NO es el texto plano
        assertThat(guardado).isNotEmpty().isNotEqualTo(plano);

        // 2) al leerlo, se descifra al valor original
        when(repo.findById("ibmi.password"))
                .thenReturn(Optional.of(new Configuracion("ibmi.password", guardado)));
        assertThat(configuracionService.getSecreto("ibmi.password")).isEqualTo(plano);
    }

    @Test
    @DisplayName("AES: un secreto vacío se guarda vacío (no se cifra cadena vacía)")
    void aes_secretoVacioQuedaVacio() {
        ArgumentCaptor<Configuracion> captor = ArgumentCaptor.forClass(Configuracion.class);
        configuracionService.setSecreto("ts4300.password", "");
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getValor()).isEmpty();
    }
}
