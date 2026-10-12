package com.arnaldo.tesis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String encargado;
    private String telefono;

    private String email;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    @Column(name = "promedio_gb_backup")
    private Integer promedioGbBackup = 0;

    @Column(name = "dias_entre_backups")
    private Integer diasEntreBackups = 0;

    // Baja lógica: un cliente inhabilitado no se elimina (se preserva la trazabilidad)
    @Column(name = "activo")
    private Boolean activo = true;

    public Cliente() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEncargado() { return encargado; }
    public void setEncargado(String encargado) { this.encargado = encargado; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Integer getPromedioGbBackup() { return promedioGbBackup; }
    public void setPromedioGbBackup(Integer promedioGbBackup) { this.promedioGbBackup = promedioGbBackup; }

    public Integer getDiasEntreBackups() { return diasEntreBackups; }
    public void setDiasEntreBackups(Integer diasEntreBackups) { this.diasEntreBackups = diasEntreBackups; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
