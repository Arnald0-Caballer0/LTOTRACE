package com.arnaldo.tesis;

import jakarta.persistence.*;

/** Cada inconsistencia encontrada en una auditoría de conciliación. */
@Entity
@Table(name = "auditoria_inventario_detalle")
public class AuditoriaInventarioDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditoria_id")
    private AuditoriaInventario auditoria;

    @Column(length = 20)
    private String barcode;

    @Column(length = 30)
    private String tipo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "ubicacion_bd", length = 30)
    private String ubicacionBd;

    @Column(name = "ubicacion_fisica", length = 30)
    private String ubicacionFisica;

    public AuditoriaInventarioDetalle() {}

    public AuditoriaInventarioDetalle(String barcode, String tipo, String descripcion, String ubicacionBd, String ubicacionFisica) {
        this.barcode = barcode;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.ubicacionBd = ubicacionBd;
        this.ubicacionFisica = ubicacionFisica;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AuditoriaInventario getAuditoria() { return auditoria; }
    public void setAuditoria(AuditoriaInventario auditoria) { this.auditoria = auditoria; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUbicacionBd() { return ubicacionBd; }
    public void setUbicacionBd(String ubicacionBd) { this.ubicacionBd = ubicacionBd; }
    public String getUbicacionFisica() { return ubicacionFisica; }
    public void setUbicacionFisica(String ubicacionFisica) { this.ubicacionFisica = ubicacionFisica; }
}
