package com.arnaldo.tesis;

import java.time.LocalDateTime;

/**
 * Representa una fila de la tabla "movimientos".
 * Es la bitácora de trazabilidad: cada vez que una cinta cambia de estado,
 * se ubica, se relaciona o se retira, queda registrado aquí.
 *
 * Es un POJO simple (sin JPA) para mantener el mismo estilo JDBC del CintaDAO.
 */
public class Movimiento {

    private Long id;
    private String barcode;
    private String tipoMovimiento;
    private LocalDateTime fecha;
    private String observacion;
    private String comprobanteDigital;

    public Movimiento() {
    }

    public Movimiento(Long id, String barcode, String tipoMovimiento,
                      LocalDateTime fecha, String observacion, String comprobanteDigital) {
        this.id = id;
        this.barcode = barcode;
        this.tipoMovimiento = tipoMovimiento;
        this.fecha = fecha;
        this.observacion = observacion;
        this.comprobanteDigital = comprobanteDigital;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getComprobanteDigital() { return comprobanteDigital; }
    public void setComprobanteDigital(String comprobanteDigital) { this.comprobanteDigital = comprobanteDigital; }
}
