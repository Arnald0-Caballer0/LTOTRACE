package com.arnaldo.tesis;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Módulo de Configuración del sistema. Solo accesible para ADMIN.
 * Sección 1: gestión de tecnologías LTO (capacidades parametrizables).
 */
@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    @Autowired
    private TecnologiaLtoRepository tecnologiaRepo;

    @Autowired
    private CintaDAO cintaDAO;

    @Autowired
    private ConfiguracionService cfg;

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private AuditoriaService auditoria;

    private boolean esAdmin(HttpSession session) {
        Usuario u = (session != null) ? (Usuario) session.getAttribute(LoginController.SESSION_USER) : null;
        return u != null && "ADMIN".equals(u.getRolCodigo());
    }

    // ---- Listado de tecnologías ----
    @GetMapping("/tecnologias")
    public String tecnologias(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/?denegado";

        List<TecnologiaLto> lista = tecnologiaRepo.findAllByOrderByGeneracionAsc();
        // Cuántas cintas usa cada tecnología (para mostrar y para el borrado seguro)
        Map<String, Integer> uso = new HashMap<>();
        for (TecnologiaLto t : lista) {
            uso.put(t.getNombre(), cintaDAO.contarPorTecnologia(t.getNombre()));
        }
        model.addAttribute("tecnologias", lista);
        model.addAttribute("uso", uso);
        return "tecnologias";
    }

    // ---- Conexión a la librería (ver / guardar) ----
    @GetMapping("/libreria")
    public String libreria(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/?denegado";
        model.addAttribute("ts_ip", cfg.get("ts4300.ip"));
        model.addAttribute("ts_usuario", cfg.get("ts4300.usuario"));
        model.addAttribute("ts_drive", cfg.get("ts4300.drive"));
        model.addAttribute("ts_particion", cfg.get("ts4300.particion"));
        model.addAttribute("ts_passSet", cfg.tieneSecreto("ts4300.password"));
        model.addAttribute("ibmi_host", cfg.get("ibmi.host"));
        model.addAttribute("ibmi_usuario", cfg.get("ibmi.usuario"));
        model.addAttribute("ibmi_passSet", cfg.tieneSecreto("ibmi.password"));
        return "libreria";
    }

    @PostMapping("/libreria/guardar")
    public String guardarLibreria(@RequestParam java.util.Map<String, String> p, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/?denegado";

        cfg.set("ts4300.ip", p.get("ts_ip"));
        cfg.set("ts4300.usuario", p.get("ts_usuario"));
        cfg.set("ts4300.drive", p.get("ts_drive"));
        cfg.set("ts4300.particion", p.get("ts_particion"));
        if (p.get("ts_password") != null && !p.get("ts_password").isBlank())
            cfg.setSecreto("ts4300.password", p.get("ts_password"));

        cfg.set("ibmi.host", p.get("ibmi_host"));
        cfg.set("ibmi.usuario", p.get("ibmi_usuario"));
        if (p.get("ibmi_password") != null && !p.get("ibmi_password").isBlank())
            cfg.setSecreto("ibmi.password", p.get("ibmi_password"));
        auditoria.registrar("CONFIG_LIBRERIA", "Actualizó la configuración de conexión (TS4300 / IBM i)");
        return "redirect:/configuracion/libreria?ok=1";
    }

    // ---- Crear o editar ----
    @PostMapping("/tecnologias/guardar")
    public String guardar(@RequestParam(value = "id", required = false) Long id,
                          @RequestParam("nombre") String nombre,
                          @RequestParam(value = "generacion", required = false) Integer generacion,
                          @RequestParam("capacidadGb") Integer capacidadGb,
                          HttpSession session) {

        if (!esAdmin(session)) return "redirect:/?denegado";

        String nom = (nombre != null) ? nombre.trim() : "";
        if (nom.isEmpty() || capacidadGb == null || capacidadGb <= 0) {
            return "redirect:/configuracion/tecnologias?error=datos";
        }

        TecnologiaLto t;
        if (id != null) {
            t = tecnologiaRepo.findById(id).orElse(new TecnologiaLto());
        } else {
            // Al crear, verificar que el nombre no exista
            boolean existe = tecnologiaRepo.findAll().stream()
                    .anyMatch(x -> x.getNombre().equalsIgnoreCase(nom));
            if (existe) return "redirect:/configuracion/tecnologias?error=existe";
            t = new TecnologiaLto();
        }
        t.setNombre(nom);
        t.setGeneracion(generacion);
        t.setCapacidadGb(capacidadGb);
        tecnologiaRepo.save(t);
        auditoria.registrar("CONFIG_TECNOLOGIA",
                (id != null ? "Editó" : "Creó") + " la tecnología " + t.getNombre() + " (" + t.getCapacidadGb() + " GB)");
        return "redirect:/configuracion/tecnologias?ok=" + (id != null ? "editado" : "creado");
    }

    // ---- Eliminar (solo si ninguna cinta la usa) ----
    @PostMapping("/tecnologias/{id}/eliminar")
    public String eliminar(@PathVariable("id") Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/?denegado";

        Optional<TecnologiaLto> opt = tecnologiaRepo.findById(id);
        if (opt.isPresent()) {
            int enUso = cintaDAO.contarPorTecnologia(opt.get().getNombre());
            if (enUso > 0) {
                return "redirect:/configuracion/tecnologias?error=enuso";
            }
            tecnologiaRepo.deleteById(id);
            auditoria.registrar("CONFIG_TECNOLOGIA", "Eliminó una tecnología LTO");
        }
        return "redirect:/configuracion/tecnologias?ok=eliminado";
    }

    // ---- Probar conexiones (AJAX desde la pantalla) ----
    @PostMapping("/libreria/probar/ts4300")
    @ResponseBody
    public java.util.Map<String, Object> probarTs4300(HttpSession session) {
        if (!esAdmin(session)) return java.util.Map.of("ok", false);
        return java.util.Map.of("ok", libraryService.probarConexion());
    }

    @PostMapping("/libreria/probar/ibmi")
    @ResponseBody
    public java.util.Map<String, Object> probarIbmI(HttpSession session) {
        if (!esAdmin(session)) return java.util.Map.of("ok", false);
        return java.util.Map.of("ok", IbmIConnection.probar());
    }

}
