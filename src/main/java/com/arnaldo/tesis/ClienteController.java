package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Controller
public class ClienteController {

    // Inyectamos la conexión a la BD correctamente sin usar "new"
    @Autowired
    private ClienteDAO clienteDAO;
    @Autowired
    private PermisoService permisoService;
    @Autowired
    private AuditoriaService auditoria;

    // Este es el método que carga tu pantalla del 30/70
    @GetMapping("/clientes")
    public String moduloClientes(@org.springframework.web.bind.annotation.RequestParam(value = "op", required = false) String op,
                                 Model model, HttpSession session) {
        if ("retiro".equals(op) && !permisoService.puedeRetirar(session)) {
            return "redirect:/clientes?op=inventario&denegado";
        }
        java.util.Set<Long> permitidos = permisoService.clientesPermitidos(session);
        model.addAttribute("listaClientes", permisoService.filtrarClientes(clienteDAO.findAll(), permitidos));
        model.addAttribute("puedeRetirar", permisoService.puedeRetirar(session));
        model.addAttribute("clienteCacheado", session.getAttribute("clienteCacheado"));
        return "clientes";
    }
    // ... tu código anterior de @GetMapping("/clientes") ...

    // ¡NUEVO! Añade tu CintaDAO (el antiguo) aquí arriba
    @Autowired
    private CintaDAO cintaDAO;

    // Esta es la "puerta" (API) que JavaScript va a llamar para pedir las cintas
    @GetMapping("/api/cintas-cliente")
    @ResponseBody // Esto hace que Spring devuelva datos (JSON) en vez de una página HTML
    public List<Cinta> obtenerCintasDelCliente(@org.springframework.web.bind.annotation.RequestParam("id") int clienteId, HttpSession session) {
        if (!permisoService.puedeVerCliente(session, (long) clienteId)) {
            return new java.util.ArrayList<>();
        }
        return cintaDAO.filtrarPorCliente(clienteId);
    }
    // Esta API recibe la lista de barcodes desde JavaScript y llama a tu DAO

    // API para crear o actualizar un cliente
    @PostMapping("/api/guardar-cliente")
    @ResponseBody
    public String guardarCliente(@RequestBody Cliente cliente) {
        try {
            // save() es inteligente: si el ID viene null, inserta. Si viene con número, actualiza.
            clienteDAO.save(cliente);
            return "{\"status\":\"success\"}";
        } catch (Exception e) {
            System.err.println("Error al guardar cliente: " + e.getMessage());
            return "{\"status\":\"error\"}";
        }
    }

    // API para cambiar el estado de la cinta al instante
    @PostMapping("/api/actualizar-ubicacion")
    @ResponseBody
    public String actualizarUbicacionCinta(
            @org.springframework.web.bind.annotation.RequestParam("barcode") String barcode,
            @org.springframework.web.bind.annotation.RequestParam("ubicacion") String ubicacion) {
        try {
            cintaDAO.actualizarUbicacion(barcode, ubicacion);
            return "{\"status\":\"success\"}";
        } catch (Exception e) {
            return "{\"status\":\"error\"}";
        }
    }

    @Autowired
    private ComprobanteService comprobanteService;

    // Procesa el retiro: genera el PDF, lo guarda en la base y lo descarga
    @PostMapping("/api/comprobante-retiro")
    public ResponseEntity<byte[]> comprobanteRetiro(
            @org.springframework.web.bind.annotation.RequestParam("clienteId") Long clienteId,
            @RequestBody List<String> barcodes,
            HttpSession session) {

        if (!permisoService.puedeRetirar(session)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        // 1. Datos para el acta (antes de retirar)
        Cliente cliente = clienteDAO.findById(clienteId).orElse(null);
        List<Cinta> cintas = cintaDAO.buscarPorBarcodes(barcodes);

        // 2. Generar el PDF
        byte[] pdf = comprobanteService.generarActaRetiro(cliente, cintas, UsuarioActual.get());

        // 3. Guardar el PDF (Base64) y retirar las cintas en un solo paso
        String base64 = java.util.Base64.getEncoder().encodeToString(pdf);
        cintaDAO.retirarCintas(barcodes, base64);

        // 4. Devolver el PDF para descarga
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "acta_retiro.pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // Guarda (o limpia) el cliente seleccionado en la sesión, para recordarlo en el módulo
    @PostMapping("/api/cliente-cacheado")
    @ResponseBody
    public String cachearCliente(@org.springframework.web.bind.annotation.RequestParam("id") Integer clienteId,
                                 HttpSession session) {
        if (clienteId == null || clienteId == 0) {
            session.removeAttribute("clienteCacheado");
        } else if (permisoService.puedeVerCliente(session, clienteId.longValue())) {
            session.setAttribute("clienteCacheado", clienteId);
        }
        return "{\"status\":\"ok\"}";
    }

    @PostMapping("/api/cliente-estado")
    @ResponseBody
    public String cambiarEstadoCliente(
            @org.springframework.web.bind.annotation.RequestParam("id") Long id,
            @org.springframework.web.bind.annotation.RequestParam("activo") boolean activo,
            HttpSession session) {
        Usuario u = permisoService.usuarioActual(session);
        if (u == null || !u.isAdmin()) {
            return "{\"status\":\"error\",\"mensaje\":\"Solo un administrador puede inhabilitar clientes.\"}";
        }
        try {
            Cliente c = clienteDAO.findById(id).orElse(null);
            if (c == null) return "{\"status\":\"error\",\"mensaje\":\"Cliente no encontrado.\"}";
            c.setActivo(activo);
            clienteDAO.save(c);
            auditoria.registrar(activo ? "CLIENTE_HABILITADO" : "CLIENTE_INHABILITADO",
                    "Cliente " + c.getNombre() + (activo ? " habilitado" : " inhabilitado"));
            return "{\"status\":\"success\"}";
        } catch (Exception e) {
            return "{\"status\":\"error\"}";
        }
    }

}
