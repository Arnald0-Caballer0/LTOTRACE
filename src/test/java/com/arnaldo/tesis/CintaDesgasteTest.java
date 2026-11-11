package com.arnaldo.tesis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP-12 — Indicador de uso de los medios LTO (RF-04).
 * Vida nominal según IBM: 20.000 ciclos de carga y descarga (Ultrium 3 a 8)
 * y 100.000 (Ultrium 9). Verde por debajo del 70 %, amarillo (advertencia)
 * desde el 70 % y rojo con la leyenda "Reemplazar" al 100 %.
 */
@DisplayName("CP-12 · Indicador de uso de la cinta")
class CintaDesgasteTest {

    private Cinta cinta(int usos, String tecnologia) {
        Cinta c = new Cinta("CC0001L8", "PENDIENTE", "LIBRERIA", null, null, tecnologia);
        c.setContadorUsos(usos);
        return c;
    }

    @Test
    @DisplayName("Una cinta nueva tiene 0 % de uso y nivel normal")
    void cintaNueva() {
        Cinta c = cinta(0, "LTO-8");
        assertThat(c.getPorcentajeDesgaste()).isZero();
        assertThat(c.getColorDesgaste()).isEqualTo("bg-success");
        assertThat(c.getTextoDesgaste()).isEqualTo("Usos: 0 / 20000");
    }

    @Test
    @DisplayName("Con 13.999 usos (69 %) sigue en nivel normal")
    void debajoDelUmbralDeAdvertencia() {
        Cinta c = cinta(13_999, "LTO-8");
        assertThat(c.getPorcentajeDesgaste()).isEqualTo(69);
        assertThat(c.getColorDesgaste()).isEqualTo("bg-success");
        assertThat(Cinta.nivelUso(13_999, "LTO-8")).isEqualTo("SALUDABLE");
    }

    @Test
    @DisplayName("Con 14.000 usos (70 %) emite la alerta de advertencia")
    void umbralDeAdvertencia() {
        Cinta c = cinta(14_000, "LTO-8");
        assertThat(c.getPorcentajeDesgaste()).isEqualTo(70);
        assertThat(c.getColorDesgaste()).isEqualTo("bg-warning");
        assertThat(c.getTextoDesgaste()).doesNotContain("Reemplazar");
        assertThat(Cinta.nivelUso(14_000, "LTO-8")).isEqualTo("ADVERTENCIA");
    }

    @Test
    @DisplayName("Con 20.000 usos (100 %) indica que la cinta debe reemplazarse")
    void vidaNominalAlcanzada() {
        Cinta c = cinta(20_000, "LTO-8");
        assertThat(c.getPorcentajeDesgaste()).isEqualTo(100);
        assertThat(c.getColorDesgaste()).isEqualTo("bg-danger");
        assertThat(c.getTextoDesgaste()).isEqualTo("Usos: 20000 / 20000 · Reemplazar");
        assertThat(Cinta.nivelUso(20_000, "LTO-8")).isEqualTo("REEMPLAZAR");
    }

    @Test
    @DisplayName("Por encima de la vida nominal el porcentaje se limita al 100 %")
    void porcentajeLimitado() {
        assertThat(cinta(25_000, "LTO-8").getPorcentajeDesgaste()).isEqualTo(100);
    }

    @Test
    @DisplayName("LTO-9 usa una vida nominal de 100.000 ciclos")
    void lto9() {
        assertThat(Cinta.ciclosNominales("LTO-9")).isEqualTo(100_000);
        assertThat(cinta(20_000, "LTO-9").getPorcentajeDesgaste()).isEqualTo(20);
        assertThat(Cinta.ciclosNominales("LTO-M8")).isEqualTo(20_000);
        assertThat(Cinta.ciclosNominales(null)).isEqualTo(20_000);
    }
}
