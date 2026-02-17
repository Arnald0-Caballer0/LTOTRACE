package com.arnaldo.tesis;

public class Cinta {
    private String barcode;
    private String volumeId;
    private String ubicacion;
    private String estado;
    private String nombreCliente;
    private String tecnologia; // 👈 LTO-6, LTO-7, etc.

    public Cinta(String barcode, String volumeId, String ubicacion, String estado, String nombreCliente, String tecnologia) {
        this.barcode = barcode;
        this.volumeId = volumeId;
        this.ubicacion = ubicacion;
        this.estado = estado;
        this.nombreCliente = nombreCliente;
        this.tecnologia = tecnologia;
    }

    // Getters
    public String getBarcode() { return barcode; }
    public String getVolumeId() { return volumeId; }
    public String getUbicacion() { return ubicacion; }
    public String getEstado() { return estado; }
    public String getNombreCliente() { return nombreCliente; }
    public String getTecnologia() { return tecnologia; } // 👈 Agregar este
}