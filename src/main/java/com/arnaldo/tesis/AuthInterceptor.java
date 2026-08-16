package com.arnaldo.tesis;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Antes de atender cualquier petición protegida, verifica que haya
 * un usuario en la sesión. Si no lo hay, redirige a /login.
 * Las rutas públicas (login, recursos estáticos) se excluyen en WebConfig.
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession(false);
        boolean logueado = (session != null && session.getAttribute(LoginController.SESSION_USER) != null);

        if (logueado) {
            return true; // Deja pasar
        }

        // No autenticado: lo mandamos al login
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
