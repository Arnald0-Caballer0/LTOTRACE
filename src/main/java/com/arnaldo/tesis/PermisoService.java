package com.arnaldo.tesis;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centraliza el "alcance de clientes" de cada usuario.
 * El ADMIN o quien "ve todos los clientes" no tiene restricción (devuelve null).
 */
@Service
public class PermisoService {

    public Usuario usuarioActual(HttpSession session) {
        return (session != null) ? (Usuario) session.getAttribute(LoginController.SESSION_USER) : null;
    }

    /**
     * IDs de clientes que el usuario puede ver.
     * - null  => sin restricción (ve todos)
     * - set   => solo esos clientes (puede estar vacío = no ve ninguno)
     */
    public Set<Long> clientesPermitidos(HttpSession session) {
        Usuario u = usuarioActual(session);
        if (u == null || u.isAdmin() || Boolean.TRUE.equals(u.getVeTodosClientes())) {
            return null;
        }
        Set<Long> ids = new HashSet<>();
        for (Cliente c : u.getClientesAsignados()) ids.add(c.getId());
        return ids;
    }

    /** ¿El usuario puede ver los datos de este cliente? */
    public boolean puedeVerCliente(HttpSession session, Long clienteId) {
        Set<Long> permitidos = clientesPermitidos(session);
        return permitidos == null || (clienteId != null && permitidos.contains(clienteId));
    }

    /** Filtra una lista de clientes según el alcance (o la devuelve entera si ve todos). */
    public List<Cliente> filtrarClientes(List<Cliente> todos, Set<Long> permitidos) {
        if (permitidos == null) return todos;
        return todos.stream()
                .filter(c -> permitidos.contains(c.getId()))
                .collect(Collectors.toList());
    }

    // ---- Capacidades (el ADMIN siempre puede) ----
    public boolean puedeMover(HttpSession session) {
        Usuario u = usuarioActual(session);
        return u != null && (u.isAdmin() || Boolean.TRUE.equals(u.getPuedeMoverCintas()));
    }
    public boolean puedeCrear(HttpSession session) {
        Usuario u = usuarioActual(session);
        return u != null && (u.isAdmin() || Boolean.TRUE.equals(u.getPuedeCrearCintas()));
    }
    public boolean puedeRetirar(HttpSession session) {
        Usuario u = usuarioActual(session);
        return u != null && (u.isAdmin() || Boolean.TRUE.equals(u.getPuedeRetirarCintas()));
    }
}
