package com.arnaldo.tesis;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Responde cuando Spring Security rechaza una solicitud por token CSRF ausente o inválido.
 *
 *  - Si la sesión ya no existe (venció), se comporta igual que AuthInterceptor:
 *    401 en JSON para la API y redirección al login para la navegación normal.
 *  - Si la sesión existe, la solicitud no trae un token válido: se responde 403.
 *    La cabecera X-LTO-Motivo permite que lto-ui.js muestre un aviso claro.
 */
public class CsrfRechazoHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        boolean esApi = request.getRequestURI().startsWith(request.getContextPath() + "/api/");
        HttpSession session = request.getSession(false);
        boolean conSesion = session != null && session.getAttribute(LoginController.SESSION_USER) != null;

        if (!conSesion) {
            if (esApi) {
                escribirJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Tu sesión expiró. Iniciá sesión nuevamente.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login?expirada");
            }
            return;
        }

        response.setHeader("X-LTO-Motivo", "csrf");
        if (esApi) {
            escribirJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "La solicitud no pudo validarse. Recargá la página e intentá nuevamente.");
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "La solicitud no pudo validarse. Recargá la página e intentá nuevamente.");
        }
    }

    private void escribirJson(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"error\",\"mensaje\":\"" + mensaje + "\"}");
    }
}
