package com.arnaldo.tesis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
@Entity
@Table(name = "cintas")
public class Cinta {

    @Id
    private String barcode;

    @Column(name = "volume_id")
    private String volumeId;

    private String ubicacion;
    private String estado;
    private String tecnologia; // Mantenemos  campo de tecnología

    // --- NUEVO: La relación con el módulo de clientes ---
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // --- ANTIGUO: Mantenemos este campo para no romper tu otro código ---
    // @Transient hace que la base de datos lo ignore por completo
    @Transient
    private String nombreCliente;

    @Column(name = "dia_semana")
    private String diaSemana;

    @UpdateTimestamp
    @Column(name = "ultima_modificacion", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime ultimaModificacion;

    // 1. Constructor vacío (Obligatorio para que Spring JPA funcione)
    public Cinta() {
    }

    // 2. Tu constructor original INTACTO (Para que LibraryService/IbmlConnection no fallen)
    public Cinta(String barcode, String volumeId, String ubicacion, String estado, String nombreCliente, String tecnologia) {
        this.barcode = barcode;
        this.volumeId = volumeId;
        this.ubicacion = ubicacion;
        this.estado = estado;
        this.nombreCliente = nombreCliente;
        this.tecnologia = tecnologia;
    }

    // --- Getters y Setters ---
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getVolumeId() { return volumeId; }
    public void setVolumeId(String volumeId) { this.volumeId = volumeId; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTecnologia() { return tecnologia; }
    public void setTecnologia(String tecnologia) { this.tecnologia = tecnologia; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public LocalDateTime getUltimaModificacion() {
        return ultimaModificacion;
    }

    public void setUltimaModificacion(LocalDateTime ultimaModificacion) {
        this.ultimaModificacion = ultimaModificacion;
    }
}