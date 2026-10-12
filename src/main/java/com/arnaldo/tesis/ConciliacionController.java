package com.arnaldo.tesis;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ConciliacionController {

    @Autowired
    private ConciliacionService conciliacionService;

    @Autowired
    private AuditoriaInventarioRepository auditoriaRepo;

    @Autowired
    private AuditoriaService auditoria;

    private boolean esAdmin(HttpSession session) {
        Usuario u = (session != null) ? (Usuario) session.getAttribute(LoginController.SESSION_USER) : null;
        return u != null && u.isAdmin();
    }

    // Etiqueta + color de badge según el tipo de inconsistencia
    private String[] infoTipo(String tipo) {
        switch (tipo) {
            case "DESCONOCIDA": return new String[]{"Cinta desconocida", "bg-danger"};
            case "NO_LOCALIZADA": return new String[]{"No localizada", "bg-warning text-dark"};
            case "UBICACION_INCORRECTA": return new String[]{"Ubicación incorrecta", "bg-warning text-dark"};
            case "RETIRADA_EN_LIBRERIA": return new String[]{"Retirada en librería", "bg-danger"};
            case "SIN_VOLUME_ID": return new String[]{"Sin Volume ID", "bg-secondary"};
            case "VOLUME_ID_DUPLICADO": return new String[]{"Volume ID duplicado", "bg-danger"};
            default: return new String[]{tipo, "bg-dark"};
        }
    }

    @GetMapping("/auditoria-inventario")
    public String ver(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/?denegado";

        List<AuditoriaInventario> historial = auditoriaRepo.findTop50ByOrderByFechaDesc();
        model.addAttribute("historial", historial);

        if (!historial.isEmpty()) {
            AuditoriaInventario ult = historial.get(0);
            model.addAttribute("auditoria", ult);

            List<Map<String, String>> dets = new ArrayList<>();
            Map<String, Long> conteo = new LinkedHashMap<>();
            for (AuditoriaInventarioDetalle d : ult.getDetalles()) {
                String[] info = infoTipo(d.getTipo());
                Map<String, String> m = new HashMap<>();
                m.put("barcode", d.getBarcode());
                m.put("tipoLabel", info[0]);
                m.put("tipoClass", info[1]);
                m.put("descripcion", d.getDescripcion());
                m.put("ubicacionBd", d.getUbicacionBd());
                m.put("ubicacionFisica", d.getUbicacionFisica());
                dets.add(m);
                conteo.merge(info[0], 1L, Long::sum);
            }
            model.addAttribute("detalles", dets);
            model.addAttribute("conteoTipos", conteo);
        }
        return "auditoria-inventario";
    }

    @PostMapping("/api/auditoria-inventario/ejecutar")
    @ResponseBody
    public Map<String, Object> ejecutar(HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        if (!esAdmin(session)) {
            resp.put("status", "error");
            resp.put("mensaje", "Solo un administrador puede ejecutar la auditoría.");
            return resp;
        }

        Usuario u = (Usuario) session.getAttribute(LoginController.SESSION_USER);
        try {
            AuditoriaInventario aud = conciliacionService.ejecutar(u.getUsername());
            auditoria.registrar(u.getUsername(), "AUDITORIA_INVENTARIO",
                    "Ejecutó auditoría de inventario: " + aud.getTotalInconsistencias() + " inconsistencias", null);
            resp.put("status", "success");
            resp.put("totalInconsistencias", aud.getTotalInconsistencias());
        } catch (IllegalStateException ex) {
            // Por ejemplo, TS4300 no disponible: no se genera una auditoría inválida
            resp.put("status", "error");
            resp.put("mensaje", ex.getMessage());
        }
        return resp;
    }
}
