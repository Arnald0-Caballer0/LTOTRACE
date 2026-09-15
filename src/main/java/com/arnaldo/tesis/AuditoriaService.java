package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * Registra eventos de auditoría. Nunca debe romper el flujo principal:
 * si falla el guardado, solo se loguea el error.
 */
@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository repo;

    /** Evento del usuario actual (para acciones dentro del sistema). */
    public void registrar(String tipoEvento, String detalle) {
        guardar(UsuarioActual.get(), tipoEvento, detalle, null);
    }

    /** Evento con usuario e IP explícitos (para login/logout). */
    public void registrar(String usuario, String tipoEvento, String detalle, String ip) {
        guardar(usuario, tipoEvento, detalle, ip);
    }

    private void guardar(String usuario, String tipoEvento, String detalle, String ip) {
        try {
            Auditoria a = new Auditoria();
            a.setFecha(LocalDateTime.now());
            a.setUsuario(usuario);
            a.setTipoEvento(tipoEvento);
            a.setDetalle(detalle);
            a.setIp(ip);
            repo.save(a);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría: " + e.getMessage());
        }
    }
}
