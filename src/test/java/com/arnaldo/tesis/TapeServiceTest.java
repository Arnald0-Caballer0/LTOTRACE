package com.arnaldo.tesis;

import com.ibm.as400.access.AS400Text;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP-09 — Relación del Volume ID a partir del mensaje CPC6778 del IBM i.
 * Se prueba de forma aislada la extracción del identificador del volumen
 * (sin necesidad de conexión al AS400):
 *  - desde los datos de sustitución del mensaje (fuente principal, independiente del idioma);
 *  - desde el texto del mensaje (respaldo).
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

    // ---------- Fuente principal: datos de sustitución (EBCDIC) ----------

    @Test
    @DisplayName("Obtiene el Volume ID desde los datos de sustitución, sin depender del idioma")
    void sustitucion_devuelveVolumeId() {
        // &1 = volumen (6 caracteres) y &2 = dispositivo (10 caracteres), en EBCDIC CCSID 37
        byte[] datos = new AS400Text(16, 37).toBytes("TAP002TAP07     ");
        assertThat(TapeService.volumeIdDesdeSustitucion(datos, 37)).isEqualTo("TAP002");
    }

    @Test
    @DisplayName("Quita los espacios de relleno de un Volume ID corto")
    void sustitucion_quitaRelleno() {
        byte[] datos = new AS400Text(16, 37).toBytes("A1    TAP07     ");
        assertThat(TapeService.volumeIdDesdeSustitucion(datos, 37)).isEqualTo("A1");
    }

    @Test
    @DisplayName("Devuelve null si no hay datos de sustitución (se usa el texto como respaldo)")
    void sustitucion_sinDatos_devuelveNull() {
        assertThat(TapeService.volumeIdDesdeSustitucion(null, 37)).isNull();
        assertThat(TapeService.volumeIdDesdeSustitucion(new byte[2], 37)).isNull();
    }
}
