package com.arnaldo.tesis;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Gestión de usuarios del sistema. Solo accesible para usuarios con rol ADMIN.
 * Las contraseñas se hashean con BCrypt antes de guardarse (nunca en texto plano).
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditoriaService auditoria;

    @Autowired
    private RolRepository rolRepository;

    /** Verifica que el usuario en sesión tenga rol ADMIN. */
    private boolean esAdmin(HttpSession session) {
        Usuario u = (session != null)
                ? (Usuario) session.getAttribute(LoginController.SESSION_USER)
                : null;
        return u != null && "ADMIN".equals(u.getRolCodigo());
    }

    // Lista de usuarios
    @GetMapping
    public String listar(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/?denegado";
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "usuarios";
    }

    // Crear un usuario nuevo
    @PostMapping("/crear")
    public String crear(@RequestParam("username") String username,
                        @RequestParam(value = "nombreCompleto", required = false) String nombreCompleto,
                        @RequestParam("password") String password,
                        @RequestParam("rol") String rol,
                        HttpSession session) {

        if (!esAdmin(session)) return "redirect:/?denegado";

        String user = (username != null) ? username.trim() : "";
        if (user.isEmpty() || password == null || password.isBlank()) {
            return "redirect:/usuarios?error=datos";
        }

        // El username no puede repetirse (ni siquiera con usuarios desactivados)
        boolean yaExiste = usuarioRepository.findAll().stream()
                .anyMatch(x -> x.getUsername().equalsIgnoreCase(user));
        if (yaExiste) {
            return "redirect:/usuarios?error=existe";
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsername(user);
        nuevo.setNombreCompleto(nombreCompleto);
        nuevo.setPasswordHash(authService.hashear(password)); // Hash BCrypt automático
        String codigoRol = "ADMIN".equals(rol) ? "ADMIN" : "OPERADOR";
        nuevo.setRol(rolRepository.findByCodigo(codigoRol).orElse(null));
        nuevo.setActivo(true);
        nuevo.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(nuevo);
        auditoria.registrar("USUARIO_CREADO", "Usuario creado: " + nuevo.getUsername() + " (" + nuevo.getRolCodigo() + ")");

        return "redirect:/usuarios?ok=creado";
    }

    // Activar / desactivar un usuario (baja lógica)
    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable("id") Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/?denegado";

        Usuario actual = (Usuario) session.getAttribute(LoginController.SESSION_USER);
        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isPresent()) {
            Usuario u = opt.get();
            // Un admin no puede desactivarse a sí mismo (evita quedar bloqueado)
            if (actual != null && actual.getId().equals(u.getId())) {
                return "redirect:/usuarios?error=autodesactivar";
            }
            u.setActivo(!Boolean.TRUE.equals(u.getActivo()));
            usuarioRepository.save(u);
            auditoria.registrar("USUARIO_ESTADO", "Usuario " + u.getUsername() + (u.getActivo() ? " activado" : " desactivado"));

        }
        return "redirect:/usuarios?ok=estado";
    }

    // Resetear la contraseña de un usuario
    @PostMapping("/{id}/password")
    public String resetPassword(@PathVariable("id") Long id,
                                @RequestParam("password") String password,
                                HttpSession session) {

        if (!esAdmin(session)) return "redirect:/?denegado";
        if (password == null || password.isBlank()) return "redirect:/usuarios?error=datos";

        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isPresent()) {
            Usuario u = opt.get();
            u.setPasswordHash(authService.hashear(password));
            usuarioRepository.save(u);
            auditoria.registrar("USUARIO_PASSWORD", "Contraseña restablecida: " + u.getUsername());
        }

        return "redirect:/usuarios?ok=password";
    }
}
