package com.arnaldo.tesis;

import jakarta.persistence.*;

/**
 * Rol de usuario, normalizado en su propia tabla.
 * 'codigo' es el valor que usa el sistema para los controles de acceso (ADMIN/OPERADOR),
 * 'nombre' es el texto para mostrar, y 'descripcion' documenta el rol.
 */
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    public Rol() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
