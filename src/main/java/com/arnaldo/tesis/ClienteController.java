package com.arnaldo.tesis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private ClienteDAO clienteDAO = new ClienteDAO();

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteDAO.listarTodos());
        return "clientes"; // Buscará templates/clientes.html
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam String nombre, @RequestParam String correo) {
        clienteDAO.guardar(nombre, correo);
        return "redirect:/clientes";
    }
}