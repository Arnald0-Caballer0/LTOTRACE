package com.arnaldo.tesis;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Antes de atender cualquier petición protegida, verifica que haya
 * un usuario en la sesión. Si no lo hay, redirige a /login.
 *
 * En cada petición vuelve a leer el usuario desde la base de datos, de modo que:
 *  - los cambios de rol, permisos y alcance de clientes se aplican de inmediato
 *    (no hace falta que el usuario cierre sesión);
 *  - si un administrador desactiva la cuenta, la sesión abierta se cierra en la
 *    siguiente acción del usuario.
 *
 * Además, deja disponible el usuario actual (UsuarioActual) para que el
 * CintaDAO pueda registrar QUIÉN hizo cada movimiento, y lo limpia al final.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UsuarioRepository usuarioRepository;

    public AuthInterceptor(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession(false);
        Usuario enSesion = (session != null)
                ? (Usuario) session.getAttribute(LoginController.SESSION_USER)
                : null;

        if (enSesion != null) {
            // Releemos el usuario para trabajar siempre con sus permisos vigentes
            Usuario vigente = (enSesion.getId() != null)
                    ? usuarioRepository.findById(enSesion.getId()).orElse(null)
                    : null;

            if (vigente == null || !Boolean.TRUE.equals(vigente.getActivo())) {
                // La cuenta fue eliminada o desactivada: se cierra la sesión
                session.invalidate();
                return rechazar(request, response,
                        "Tu cuenta fue desactivada. Contactá a un administrador.",
                        "/login?deshabilitado");
            }

            session.setAttribute(LoginController.SESSION_USER, vigente);
            UsuarioActual.set(vigente.getUsername());
            return true; // Deja pasar
        }

        // Sin sesión: navegación normal va al login (el aviso de sesión expirada solo se
        // muestra cuando la sesión vence en medio de una acción, ver lto-ui.js)
        return rechazar(request, response,
                "Tu sesión expiró. Iniciá sesión nuevamente.",
                "/login");
    }

    /** Responde 401 en JSON a las llamadas de la API y redirige al login en la navegación normal. */
    private boolean rechazar(HttpServletRequest request, HttpServletResponse response,
                             String mensajeApi, String destino) throws Exception {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"mensaje\":\"" + mensajeApi + "\"}");
            return false;
        }
        response.sendRedirect(request.getContextPath() + destino);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        // Limpiamos el casillero para no dejar el usuario "pegado" en el hilo reutilizable
        UsuarioActual.limpiar();
    }
}
