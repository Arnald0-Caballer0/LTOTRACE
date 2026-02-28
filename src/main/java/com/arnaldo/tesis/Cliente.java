package com.arnaldo.tesis;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes") // Aseguramos que apunte a tu tabla real
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;      // Razón Social / Empresa
    private String encargado;   // Contacto
    private String telefono;
    private String email;       // Para el envío del acta

    // Constructor vacío obligatorio para JPA
    public Cliente() {
    }

    // --- Getters y Setters ---
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
}