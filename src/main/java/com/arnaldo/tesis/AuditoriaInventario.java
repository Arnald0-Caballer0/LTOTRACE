package com.arnaldo.tesis;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Cabecera de una auditoría de conciliación de inventario. */
@Entity
@Table(name = "auditoria_inventario")
public class AuditoriaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    @Column(length = 50)
    private String usuario;

    @Column(name = "total_analizadas")
    private Integer totalAnalizadas;

    private Integer coincidencias;

    @Column(name = "total_inconsistencias")
    private Integer totalInconsistencias;

    @OneToMany(mappedBy = "auditoria", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AuditoriaInventarioDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(AuditoriaInventarioDetalle d) {
        d.setAuditoria(this);
        this.detalles.add(d);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public Integer getTotalAnalizadas() { return totalAnalizadas; }
    public void setTotalAnalizadas(Integer totalAnalizadas) { this.totalAnalizadas = totalAnalizadas; }
    public Integer getCoincidencias() { return coincidencias; }
    public void setCoincidencias(Integer coincidencias) { this.coincidencias = coincidencias; }
    public Integer getTotalInconsistencias() { return totalInconsistencias; }
    public void setTotalInconsistencias(Integer totalInconsistencias) { this.totalInconsistencias = totalInconsistencias; }
    public List<AuditoriaInventarioDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<AuditoriaInventarioDetalle> detalles) { this.detalles = detalles; }
}
