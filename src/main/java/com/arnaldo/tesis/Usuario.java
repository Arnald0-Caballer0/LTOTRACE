package com.arnaldo.tesis;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Usuario del sistema. El rol ahora está normalizado (FK a la tabla roles).
 * Se conserva getRolCodigo() como método de compatibilidad para los controles
 * de acceso (que comparan contra "ADMIN"/"OPERADOR").
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombre_completo", length = 120)
    private String nombreCompleto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    public Usuario() {}

    // --- Rol (con métodos de compatibilidad) ---
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    /** Código del rol (ADMIN/OPERADOR) para los controles de acceso. */
    public String getRolCodigo() { return rol != null ? rol.getCodigo() : null; }

    /** Nombre para mostrar del rol (Administrador/Operador). */
    public String getRolNombre() { return rol != null ? rol.getNombre() : null; }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
