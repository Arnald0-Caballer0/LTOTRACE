package com.arnaldo.tesis;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Usuario del sistema. Incluye rol (FK), permisos, y parámetros de seguridad
 * (intentos fallidos y bloqueo). El ADMIN siempre tiene acceso total.
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

    // --- Permisos ---
    // Principio de mínimo privilegio: un usuario nuevo no tiene capacidades operativas
    // ni clientes asignados hasta que un administrador se los otorgue (el ADMIN no depende de estos campos).
    @Column(name = "ve_todos_clientes")
    private Boolean veTodosClientes = false;

    @Column(name = "puede_mover_cintas")
    private Boolean puedeMoverCintas = false;

    @Column(name = "puede_crear_cintas")
    private Boolean puedeCrearCintas = false;

    @Column(name = "puede_retirar_cintas")
    private Boolean puedeRetirarCintas = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_cliente",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "cliente_id"))
    private Set<Cliente> clientesAsignados = new HashSet<>();

    // --- Seguridad de acceso ---
    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    public Usuario() {}

    // --- Rol (compatibilidad) ---
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getRolCodigo() { return rol != null ? rol.getCodigo() : null; }
    public String getRolNombre() { return rol != null ? rol.getNombre() : null; }
    public boolean isAdmin() { return "ADMIN".equals(getRolCodigo()); }

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

    public Boolean getVeTodosClientes() { return veTodosClientes; }
    public void setVeTodosClientes(Boolean veTodosClientes) { this.veTodosClientes = veTodosClientes; }

    public Boolean getPuedeMoverCintas() { return puedeMoverCintas; }
    public void setPuedeMoverCintas(Boolean puedeMoverCintas) { this.puedeMoverCintas = puedeMoverCintas; }

    public Boolean getPuedeCrearCintas() { return puedeCrearCintas; }
    public void setPuedeCrearCintas(Boolean puedeCrearCintas) { this.puedeCrearCintas = puedeCrearCintas; }

    public Boolean getPuedeRetirarCintas() { return puedeRetirarCintas; }
    public void setPuedeRetirarCintas(Boolean puedeRetirarCintas) { this.puedeRetirarCintas = puedeRetirarCintas; }

    public Set<Cliente> getClientesAsignados() { return clientesAsignados; }
    public void setClientesAsignados(Set<Cliente> clientesAsignados) { this.clientesAsignados = clientesAsignados; }

    public Integer getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(Integer intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
}
