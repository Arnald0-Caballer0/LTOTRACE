package com.arnaldo.tesis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP-03 (complemento) — Política mínima de contraseñas.
 */
@DisplayName("CP-03 · Política de contraseñas")
class PoliticaContrasenaTest {

    @Test
    @DisplayName("Acepta una contraseña de 8+ caracteres con letras y números")
    void aceptaContrasenaValida() {
        assertThat(PoliticaContrasena.validar("Cinta2026", "operador1")).isNull();
    }

    @Test
    @DisplayName("Rechaza contraseñas cortas")
    void rechazaCorta() {
        assertThat(PoliticaContrasena.cumple("abc12", "operador1")).isFalse();
    }

    @Test
    @DisplayName("Rechaza contraseñas sin números o sin letras")
    void rechazaSinCombinacion() {
        assertThat(PoliticaContrasena.cumple("soloLetras", "operador1")).isFalse();
        assertThat(PoliticaContrasena.cumple("12345678", "operador1")).isFalse();
    }

    @Test
    @DisplayName("Rechaza contraseñas que contienen el nombre de usuario")
    void rechazaConUsuario() {
        assertThat(PoliticaContrasena.cumple("jperez2026", "jperez")).isFalse();
    }

    @Test
    @DisplayName("Rechaza contraseñas vacías o nulas")
    void rechazaVacia() {
        assertThat(PoliticaContrasena.cumple("", "operador1")).isFalse();
        assertThat(PoliticaContrasena.cumple(null, "operador1")).isFalse();
    }
}
