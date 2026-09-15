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
 * Maneja el inicio y cierre de sesión.
 * El usuario autenticado se guarda en la sesión HTTP bajo la clave "usuarioLogueado".
 */
@Controller
public class LoginController {

    public static final String SESSION_USER = "usuarioLogueado";

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditoriaService auditoria;

    // Muestra la pantalla de login
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam("username") String username,
                                @RequestParam("password") String password,
                                HttpServletRequest request,
                                Model model) {
        Usuario usuario = authService.autenticar(username, password);
        if (usuario != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_USER, usuario);
            auditoria.registrar(usuario.getUsername(), "LOGIN", "Inicio de sesión exitoso", request.getRemoteAddr());
            return "redirect:/";
        }
        auditoria.registrar(username, "LOGIN_FALLIDO", "Credenciales incorrectas", request.getRemoteAddr());
        model.addAttribute("error", "Usuario o contraseña incorrectos.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Usuario u = (Usuario) session.getAttribute(SESSION_USER);
        if (u != null) auditoria.registrar(u.getUsername(), "LOGOUT", "Cierre de sesión", null);
        session.invalidate();
        return "redirect:/login?logout";
    }
}
