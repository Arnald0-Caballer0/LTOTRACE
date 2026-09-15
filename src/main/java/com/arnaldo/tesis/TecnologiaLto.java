package com.arnaldo.tesis;

import jakarta.persistence.*;

/**
 * Tecnología de cinta LTO y su capacidad nativa.
 * Antes estaba hardcodeada en el código; ahora es parametrizable desde la base.
 */
@Entity
@Table(name = "tecnologias_lto")
public class TecnologiaLto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String nombre;

    private Integer generacion;

    @Column(name = "capacidad_gb", nullable = false)
    private Integer capacidadGb;

    public TecnologiaLto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getGeneracion() { return generacion; }
    public void setGeneracion(Integer generacion) { this.generacion = generacion; }

    public Integer getCapacidadGb() { return capacidadGb; }
    public void setCapacidadGb(Integer capacidadGb) { this.capacidadGb = capacidadGb; }
}
