package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ClienteController {

    // Inyectamos la conexión a la BD correctamente sin usar "new"
    @Autowired
    private ClienteDAO clienteDAO;

    // Este es el método que carga tu pantalla del 30/70
    @GetMapping("/clientes")
    public String moduloClientes(Model model) {

        // ¡Magia! Traemos todos los clientes reales de tu base de datos
        // y los mandamos a la vista HTML con el nombre "listaClientes"
        model.addAttribute("listaClientes", clienteDAO.findAll());

        return "clientes";
    }
    // ... tu código anterior de @GetMapping("/clientes") ...

    // ¡NUEVO! Añade tu CintaDAO (el antiguo) aquí arriba
    @Autowired
    private CintaDAO cintaDAO;

    // Esta es la "puerta" (API) que JavaScript va a llamar para pedir las cintas
    @GetMapping("/api/cintas-cliente")
    @ResponseBody // Esto hace que Spring devuelva datos (JSON) en vez de una página HTML
    public List<Cinta> obtenerCintasDelCliente(@org.springframework.web.bind.annotation.RequestParam("id") int clienteId) {

        // ¡Usamos tu método existente que ya funciona perfecto!
        return cintaDAO.filtrarPorCliente(clienteId);
    }
}