package com.arnaldo.tesis;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditoriaController {

    @Autowired
    private AuditoriaRepository repo;

    private boolean esAdmin(HttpSession session) {
        Usuario u = (session != null) ? (Usuario) session.getAttribute(LoginController.SESSION_USER) : null;
        return u != null && "ADMIN".equals(u.getRolCodigo());
    }

    @GetMapping("/auditoria")
    public String ver(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/?denegado";
        model.addAttribute("eventos", repo.findTop500ByOrderByFechaDesc());
        return "auditoria";
    }
}
