package com.arnaldo.tesis;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Maneja el inicio y cierre de sesión, con bloqueo por intentos fallidos.
 */
@Controller
public class LoginController {

    public static final String SESSION_USER = "usuarioLogueado";

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditoriaService auditoria;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam("username") String username,
                                @RequestParam("password") String password,
                                HttpServletRequest request,
                                Model model) {
        String ip = request.getRemoteAddr();
        ResultadoLogin r = authService.intentarLogin(username, password);

        switch (r.getEstado()) {
            case EXITO:
                // Cambia el identificador de la sesión al autenticarse (evita la fijación de sesión);
                // se conservan los atributos, incluido el token CSRF
                if (request.getSession(false) != null) request.changeSessionId();
                HttpSession session = request.getSession(true);
                session.setAttribute(SESSION_USER, r.getUsuario());
                auditoria.registrar(r.getUsuario().getUsername(), "LOGIN", "Inicio de sesión exitoso", ip);
                return "redirect:/";

            case BLOQUEADO_TEMPORAL:
                auditoria.registrar(username, "LOGIN_BLOQUEADO", "Cuenta bloqueada temporalmente", ip);
                model.addAttribute("error", "Cuenta bloqueada temporalmente. Intentá de nuevo en "
                        + r.getMinutosRestantes() + " minuto(s).");
                return "login";

            case DESHABILITADO:
                auditoria.registrar(username, "LOGIN_DESHABILITADO", "Acceso a cuenta deshabilitada", ip);
                model.addAttribute("error", "Usuario deshabilitado. Contactá a un administrador.");
                return "login";

            case CREDENCIALES_INVALIDAS:
            default:
                auditoria.registrar(username, "LOGIN_FALLIDO", "Credenciales incorrectas", ip);
                model.addAttribute("error", "Usuario o contraseña incorrectos.");
                return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Usuario u = (Usuario) session.getAttribute(SESSION_USER);
        if (u != null) auditoria.registrar(u.getUsername(), "LOGOUT", "Cierre de sesión", null);
        session.invalidate();
        return "redirect:/login?logout";
    }
}
