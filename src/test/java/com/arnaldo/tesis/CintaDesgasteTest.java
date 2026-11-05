package com.arnaldo.tesis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP-12 — Control de desgaste de los medios LTO (RF-04).
 * Vida útil de referencia: 250 montajes. Verde por debajo del 70 %,
 * amarillo (advertencia) desde el 70 % y rojo con la leyenda "Reemplazar" al 100 %.
 */
@DisplayName("CP-12 · Control de desgaste de la cinta")
class CintaDesgasteTest {

    private Cinta cintaConUsos(int usos) {
        Cinta c = new Cinta();
        c.setContadorUsos(usos);
        return c;
    }

    @Test
    @DisplayName("Una cinta nueva tiene 0 % de desgaste y estado saludable")
    void cintaNueva() {
        Cinta c = cintaConUsos(0);
        assertThat(c.getPorcentajeDesgaste()).isZero();
        assertThat(c.getColorDesgaste()).isEqualTo("bg-success");
        assertThat(c.getTextoDesgaste()).isEqualTo("Usos: 0 / 250");
    }

    @Test
    @DisplayName("Con 173 usos (69 %) sigue en estado saludable")
    void debajoDelUmbralDeAdvertencia() {
        Cinta c = cintaConUsos(173);
        assertThat(c.getPorcentajeDesgaste()).isEqualTo(69);
        assertThat(c.getColorDesgaste()).isEqualTo("bg-success");
    }

    @Test
    @DisplayName("Con 175 usos (70 %) emite la alerta de advertencia")
    void umbralDeAdvertencia() {
        Cinta c = cintaConUsos(175);
        assertThat(c.getPorcentajeDesgaste()).isEqualTo(70);
        assertThat(c.getColorDesgaste()).isEqualTo("bg-warning");
        assertThat(c.getTextoDesgaste()).doesNotContain("Reemplazar");
    }

    @Test
    @DisplayName("Con 250 usos (100 %) indica que la cinta debe reemplazarse")
    void vidaUtilAlcanzada() {
        Cinta c = cintaConUsos(250);
        assertThat(c.getPorcentajeDesgaste()).isEqualTo(100);
        assertThat(c.getColorDesgaste()).isEqualTo("bg-danger");
        assertThat(c.getTextoDesgaste()).isEqualTo("Usos: 250 / 250 · Reemplazar");
    }

    @Test
    @DisplayName("Por encima de la vida útil el porcentaje se limita al 100 %")
    void porcentajeLimitado() {
        assertThat(cintaConUsos(300).getPorcentajeDesgaste()).isEqualTo(100);
    }
}
