package com.arnaldo.tesis;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Antes de atender cualquier petición protegida, verifica que haya
 * un usuario en la sesión. Si no lo hay, redirige a /login.
 *
 * Además, deja disponible el usuario actual (UsuarioActual) para que el
 * CintaDAO pueda registrar QUIÉN hizo cada movimiento, y lo limpia al final.
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null)
                ? (Usuario) session.getAttribute(LoginController.SESSION_USER)
                : null;

        if (usuario != null) {
            // Dejamos disponible el usuario actual para el resto de la petición
            UsuarioActual.set(usuario.getUsername());
            return true; // Deja pasar
        }

        // No autenticado: lo mandamos al login
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        // Limpiamos el casillero para no dejar el usuario "pegado" en el hilo reutilizable
        UsuarioActual.limpiar();
    }
}
