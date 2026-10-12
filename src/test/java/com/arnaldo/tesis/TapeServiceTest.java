package com.arnaldo.tesis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP-09 — Relación del Volume ID a partir del mensaje CPC6778 del IBM i.
 * Se prueba de forma aislada la extracción del identificador del volumen
 * desde el texto del mensaje (sin necesidad de conexión al AS400).
 */
@DisplayName("CP-09 · TapeService: extracción del Volume ID (CPC6778)")
class TapeServiceTest {

    @Test
    @DisplayName("Extrae el Volume ID de un mensaje en inglés")
    void extrae_mensajeEnIngles() {
        String texto = "Volume TAP002 found on device TAP07.";
        assertThat(TapeService.extraerVolumeId(texto)).isEqualTo("TAP002");
    }

    @Test
    @DisplayName("Extrae el Volume ID de un mensaje en español")
    void extrae_mensajeEnEspanol() {
        String texto = "Volumen TAP002 se cargó en el dispositivo TAP07.";
        assertThat(TapeService.extraerVolumeId(texto)).isEqualTo("TAP002");
    }

    @Test
    @DisplayName("Quita el punto final del identificador")
    void quita_puntoFinal() {
        assertThat(TapeService.extraerVolumeId("Volume ABC123.")).isEqualTo("ABC123");
    }

    @Test
    @DisplayName("Devuelve null si el mensaje no contiene la palabra clave")
    void sinPalabraClave_devuelveNull() {
        assertThat(TapeService.extraerVolumeId("Mensaje sin el dato buscado")).isNull();
    }

    @Test
    @DisplayName("Devuelve null si el texto es nulo")
    void textoNulo_devuelveNull() {
        assertThat(TapeService.extraerVolumeId(null)).isNull();
    }
}
