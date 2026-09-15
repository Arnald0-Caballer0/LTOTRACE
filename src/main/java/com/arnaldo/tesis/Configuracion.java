package com.arnaldo.tesis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Almacén genérico de configuración (clave-valor).
 * Ej: ts4300.ip, ts4300.password (cifrada), ibmi.host, etc.
 */
@Entity
@Table(name = "configuracion")
public class Configuracion {

    @Id
    @Column(length = 60)
    private String clave;

    @Column(columnDefinition = "text")
    private String valor;

    public Configuracion() {}
    public Configuracion(String clave, String valor) { this.clave = clave; this.valor = valor; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
