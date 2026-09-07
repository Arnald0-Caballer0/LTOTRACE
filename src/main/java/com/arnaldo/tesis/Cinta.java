package com.arnaldo.tesis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "cintas")
public class Cinta {

    // Vida útil estimada en montajes (regla práctica del fabricante)
    @Transient
    private static final int VIDA_UTIL_MONTAJES = 250;

    @Id
    private String barcode;

    @Column(name = "volume_id")
    private String volumeId;

    private String ubicacion;
    private String estado;
    private String tecnologia;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Transient
    private String nombreCliente;

    @Column(name = "dia_semana")
    private String diaSemana;

    @UpdateTimestamp
    @Column(name = "ultima_modificacion", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime ultimaModificacion;

    // --- NUEVO: datos de solo-vista para desgaste y llenado ---
    @Transient
    private int contadorUsos;
    @Transient
    private Integer promedioGbBackup;   // viene del cliente
    @Transient
    private Integer diasEntreBackups;   // viene del cliente

    // 1. Constructor vacío
    public Cinta() {
    }

    // 2. Constructor original INTACTO
    public Cinta(String barcode, String volumeId, String ubicacion, String estado, String nombreCliente, String tecnologia) {
        this.barcode = barcode;
        this.volumeId = volumeId;
        this.ubicacion = ubicacion;
        this.estado = estado;
        this.nombreCliente = nombreCliente;
        this.tecnologia = tecnologia;
    }

    // ==========================================================
    // CÁLCULOS DE DESGASTE Y LLENADO
    // ==========================================================

    /** Capacidad nativa de la cinta en GB, según su tecnología. */
    public int getCapacidadGb() {
        if (tecnologia == null) return 0;
        switch (tecnologia.toUpperCase().trim()) {
            case "LTO-7": return 6000;
            case "LTO-8": return 12000;
            case "LTO-9": return 18000;
            default: return 0;
        }
    }

    // ---- Desgaste (montajes vs vida útil) ----
    public int getPorcentajeDesgaste() {
        int p = (int) Math.round(contadorUsos * 100.0 / VIDA_UTIL_MONTAJES);
        return Math.min(p, 100);
    }

    public String getColorDesgaste() {
        int p = getPorcentajeDesgaste();
        if (p >= 100) return "bg-danger";
        if (p >= 70) return "bg-warning";
        return "bg-success";
    }

    public String getTextoDesgaste() {
        String base = "Usos: " + contadorUsos + " / " + VIDA_UTIL_MONTAJES;
        if (getPorcentajeDesgaste() >= 100) base += " · Reemplazar";
        return base;
    }

    // ---- Llenado (datos escritos vs capacidad) ----
    /** Solo se puede estimar si el cliente tiene configurado el GB/backup y conocemos la capacidad. */
    public boolean isLlenadoCalculable() {
        return promedioGbBackup != null && promedioGbBackup > 0 && getCapacidadGb() > 0;
    }

    public int getGbUsados() {
        if (promedioGbBackup == null) return 0;
        return contadorUsos * promedioGbBackup;
    }

    public int getPorcentajeLlenado() {
        if (!isLlenadoCalculable()) return 0;
        int p = (int) Math.round(getGbUsados() * 100.0 / getCapacidadGb());
        return Math.min(p, 100);
    }

    public String getColorLlenado() {
        int p = getPorcentajeLlenado();
        if (p >= 100) return "bg-danger";
        if (p >= 80) return "bg-warning";
        return "bg-success";
    }

    /** Fecha estimada en que la cinta se llenará (o null si no se puede calcular). */
    public String getFechaEstimadaLlenado() {
        if (!isLlenadoCalculable() || diasEntreBackups == null || diasEntreBackups <= 0) return null;
        int usosTotales = (int) Math.ceil((double) getCapacidadGb() / promedioGbBackup);
        int usosRestantes = usosTotales - contadorUsos;
        if (usosRestantes <= 0) return null; // ya está llena
        long diasRestantes = (long) usosRestantes * diasEntreBackups;
        LocalDate fecha = LocalDate.now().plusDays(diasRestantes);
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getTextoLlenado() {
        if (!isLlenadoCalculable()) return "Configurá GB/backup del cliente";
        if (getPorcentajeLlenado() >= 100) return "Cinta llena";
        String f = getFechaEstimadaLlenado();
        return (f != null) ? ("Se llena aprox. " + f) : (getPorcentajeLlenado() + "%");
    }

    // ==========================================================
    // Getters y Setters
    // ==========================================================
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

    public LocalDateTime getUltimaModificacion() { return ultimaModificacion; }
    public void setUltimaModificacion(LocalDateTime ultimaModificacion) { this.ultimaModificacion = ultimaModificacion; }

    public int getContadorUsos() { return contadorUsos; }
    public void setContadorUsos(int contadorUsos) { this.contadorUsos = contadorUsos; }

    public Integer getPromedioGbBackup() { return promedioGbBackup; }
    public void setPromedioGbBackup(Integer promedioGbBackup) { this.promedioGbBackup = promedioGbBackup; }

    public Integer getDiasEntreBackups() { return diasEntreBackups; }
    public void setDiasEntreBackups(Integer diasEntreBackups) { this.diasEntreBackups = diasEntreBackups; }
}
