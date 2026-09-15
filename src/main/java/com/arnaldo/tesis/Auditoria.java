package com.arnaldo.tesis;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro de auditoría de eventos del sistema (seguridad):
 * inicios de sesión, cambios de usuarios, cambios de configuración, etc.
 * Complementa a "movimientos", que audita las operaciones sobre cintas.
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    @Column(length = 50)
    private String usuario;

    @Column(name = "tipo_evento", length = 40)
    private String tipoEvento;

    @Column(columnDefinition = "text")
    private String detalle;

    @Column(length = 45)
    private String ip;

    public Auditoria() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
