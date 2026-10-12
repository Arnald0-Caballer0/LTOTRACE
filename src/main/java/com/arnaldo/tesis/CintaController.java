package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.ibm.as400.access.AS400;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Controller
public class CintaController {

    private CintaDAO cintaDAO = new CintaDAO();
    @Autowired
    private ClienteDAO clienteDAO;
    @Autowired
    private LibraryService libraryService;
    @Autowired
    private PermisoService permisoService;
    @Autowired
    private ConfiguracionService cfg;


    @GetMapping("/")
    public String index(@RequestParam(name = "clienteId", required = false) Integer clienteId,
                        @RequestParam(name = "tecnologia", required = false) String tecnologia, Model model, HttpSession session) {

        java.util.Set<Long> permitidos = permisoService.clientesPermitidos(session);

        String techFilter = (tecnologia != null && !tecnologia.isEmpty()) ? tecnologia : null;

        // CAMBIO CLAVE: Aquí debe llamar a listarSemanalesManual
        model.addAttribute("cintasSemanales", cintaDAO.listarSemanalesManual(clienteId, techFilter));

        // El inventario completo (con alcance de clientes)
        model.addAttribute("cintas", cintaDAO.filtrarCintas(clienteId, techFilter, permitidos));

        model.addAttribute("clientes", permisoService.filtrarClientes(clienteDAO.findAll(), permitidos));
        model.addAttribute("clienteSeleccionado", clienteId);
        model.addAttribute("tecnologiaSeleccionada", techFilter);

        //consultamos información en la librería
        model.addAttribute("cintaEnDrive", libraryService.consultarCintaEnDrive1());
        //consultamos si el robot está trabajando
        model.addAttribute("estadoRobot", libraryService.obtenerEstadoRobot());
        model.addAttribute("puedeMover", permisoService.puedeMover(session));
        return "index";
    }


    @PostMapping("/registrar-cinta")
    public String registrar(@RequestParam(name = "barcode") String barcode,
                            @RequestParam(name = "clienteId") int clienteId,
                            @RequestParam(name = "tecnologia") String tecnologia) { //Recibimos la tecnología del form

        // Ahora pasamos la variable 'tecnologia' real, no el texto fijo "LTO-7"
        cintaDAO.registrarCinta(barcode, "PENDIENTE", clienteId, tecnologia);
        return "redirect:/";
    }

    // ==========================================================
    // RELACIONAR / REINICIAR VOLUME ID (sin recargar la pantalla)
    // ==========================================================
    @PostMapping("/api/relacionar/{barcode}")
    @ResponseBody
    public Map<String, Object> relacionarCinta(@PathVariable("barcode") String barcode, HttpSession session) {
        Map<String, Object> r = new HashMap<>();
        if (!puedeOperarCinta(session, barcode)) {
            return error(r, "No tenés permisos sobre la cinta " + barcode + ".");
        }

        // 1) Verificar en la librería que la cinta montada en el drive sea la que se quiere relacionar.
        //    Sin este control, el Volume ID de otra cinta podría quedar asignado a esta.
        String enDrive = libraryService.consultarCintaEnDrive1();
        if (enDrive == null || "ERROR_AUTH".equals(enDrive) || "SIN_DATOS".equals(enDrive)) {
            return error(r, "No se pudo consultar la librería TS4300 para verificar qué cinta está montada en el drive.");
        }
        if ("VACÍO".equals(enDrive) || enDrive.isBlank()) {
            return error(r, "No hay ninguna cinta en el drive. Montá la cinta " + barcode + " antes de relacionar su Volume ID.");
        }
        if (!enDrive.trim().equalsIgnoreCase(barcode.trim())) {
            return error(r, "En el drive está montada la cinta " + enDrive.trim() + ", no la " + barcode
                    + ". Montá la cinta correcta antes de relacionar su Volume ID.");
        }

        // 2) Leer el Volume ID desde el IBM i (CHKTAP) en el dispositivo configurado
        AS400 system = IbmIConnection.getSystem();
        if (system == null) {
            return error(r, "La conexión con el servidor IBM i no está configurada.");
        }
        String dispositivo = cfg.get("ibmi.dispositivo");
        if (dispositivo == null || dispositivo.isBlank()) dispositivo = "TAP07";
        String volumeId = new TapeService().obtenerVolumeIdDesdeIbmI(system, dispositivo.trim());
        if (volumeId == null) {
            return error(r, "El IBM i no devolvió el Volume ID en el dispositivo " + dispositivo.trim()
                    + ". Verificá que la cinta esté lista en el dispositivo y que el servidor esté disponible.");
        }

        cintaDAO.actualizarVolumeId(barcode, volumeId);
        r.put("status", "success");
        r.put("volumeId", volumeId);
        r.put("mensaje", "Volume ID " + volumeId + " relacionado correctamente con la cinta " + barcode + ".");
        return r;
    }

    @PostMapping("/api/reiniciar/{barcode}")
    @ResponseBody
    public Map<String, Object> reiniciarCinta(@PathVariable("barcode") String barcode, HttpSession session) {
        Map<String, Object> r = new HashMap<>();
        if (!puedeOperarCinta(session, barcode)) {
            return error(r, "No tenés permisos sobre la cinta " + barcode + ".");
        }
        cintaDAO.reiniciarCinta(barcode);
        r.put("status", "success");
        r.put("mensaje", "El Volume ID de la cinta " + barcode + " volvió al estado PENDIENTE.");
        return r;
    }

    /** La cinta debe pertenecer a un cliente dentro del alcance del usuario (el ADMIN ve todas). */
    private boolean puedeOperarCinta(HttpSession session, String barcode) {
        java.util.Set<Long> permitidos = permisoService.clientesPermitidos(session);
        if (permitidos == null) return true;
        return cintaDAO.filtrarCintas(null, null, permitidos).stream()
                .anyMatch(c -> c.getBarcode() != null && c.getBarcode().equalsIgnoreCase(barcode));
    }

    private Map<String, Object> error(Map<String, Object> r, String mensaje) {
        r.put("status", "error");
        r.put("mensaje", mensaje);
        return r;
    }

    // Modifica tu método de marcar para que reciba el día
    @GetMapping("/marcar-semanal/{id}")
    public String marcarSemanal(@PathVariable("id") String barcode,
                                @RequestParam(value = "dia", required = false) String dia,
                                @RequestParam(value = "clienteId", required = false) Integer clienteId,
                                @RequestParam(value = "tecnologia", required = false) String tecnologia) {
        cintaDAO.asignarDiaSemanal(barcode, dia);
        // Conservamos el cliente/tecnología para que el panel semanal no se esconda
        return "redirect:/" + construirParametros(clienteId, tecnologia);
    }

    @GetMapping("/desmarcar-semanal/{id}")
    public String desmarcarSemanal(@PathVariable("id") String barcode,
                                   @RequestParam(value = "clienteId", required = false) Integer clienteId,
                                   @RequestParam(value = "tecnologia", required = false) String tecnologia) {
        cintaDAO.quitarDiaSemanal(barcode);
        return "redirect:/" + construirParametros(clienteId, tecnologia);
    }

    // Método auxiliar para no repetir código
    private String construirParametros(Integer clienteId, String tecnologia) {
        String p = "?";
        if (clienteId != null) p += "clienteId=" + clienteId + "&";
        if (tecnologia != null && !tecnologia.isEmpty()) p += "tecnologia=" + tecnologia;
        return p;
    }

    @GetMapping("/api/estado-libreria")
    @ResponseBody
    public Map<String, String> obtenerEstadoLibreriaApi() {
        Map<String, String> data = new HashMap<>();
        data.put("cinta", libraryService.consultarCintaEnDrive1());
        data.put("robot", libraryService.obtenerEstadoRobot());
        return data;
    }

    // ==========================================
    // NUEVO ENDPOINT: Mover la cinta físicamente
    // ==========================================
    @PostMapping("/api/mover-cinta-robot")
    @ResponseBody
    public Map<String, String> moverCintaRobot(@RequestParam("barcode") String barcode, HttpSession session) {
        Map<String, String> respuesta = new HashMap<>();
        if (!permisoService.puedeMover(session)) {
            respuesta.put("status", "error");
            respuesta.put("mensaje", "No tenés permiso para mover cintas.");
            return respuesta;
        }
        if (!puedeOperarCinta(session, barcode)) {
            respuesta.put("status", "error");
            respuesta.put("mensaje", "No tenés permisos sobre la cinta " + barcode + ".");
            return respuesta;
        }

        String resultado = libraryService.moverCinta(barcode);

        if (resultado != null && resultado.startsWith("SUCCESS")) {
            if ("SUCCESS_LOAD".equals(resultado)) {
                // Carga en el drive = un uso: sube el contador y registra MONTAJE
                cintaDAO.registrarMontaje(barcode);
                respuesta.put("mensaje", "Cinta montada en el drive TS4300.");
            } else {
                // Descarga del drive: solo registra el movimiento
                cintaDAO.registrarMovimiento(barcode, "DESMONTAJE", "Cinta descargada del drive TS4300");
                respuesta.put("mensaje", "Cinta descargada del drive TS4300.");
            }
            respuesta.put("status", "success");
        } else {
            respuesta.put("status", "error");
            respuesta.put("mensaje", resultado);
        }

        return respuesta;
    }
    // ==========================================
// NUEVO ENDPOINT: Historial (trazabilidad) de una cinta
// ==========================================
    @GetMapping("/api/historial/{barcode}")
    @ResponseBody
    public List<Map<String, String>> historialCinta(@PathVariable("barcode") String barcode) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        List<Map<String, String>> salida = new ArrayList<>();

        for (Movimiento m : cintaDAO.listarMovimientos(barcode)) {
            Map<String, String> fila = new HashMap<>();
            fila.put("tipo", m.getTipoMovimiento());
            fila.put("fecha", m.getFecha() != null ? m.getFecha().format(fmt) : "");
            fila.put("observacion", m.getObservacion() != null ? m.getObservacion() : "");
            fila.put("usuario", m.getUsuario() != null ? m.getUsuario() : "");
            fila.put("id", String.valueOf(m.getId()));
            boolean tiene = m.getComprobanteDigital() != null && !m.getComprobanteDigital().isEmpty();
            fila.put("tieneComprobante", tiene ? "true" : "false");
            salida.add(fila);
        }
        return salida;
    }

    // Devuelve el comprobante PDF guardado de un movimiento
    @GetMapping("/api/comprobante/{id}")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable("id") Long id) {
        String base64 = cintaDAO.obtenerComprobante(id);
        if (base64 == null || base64.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = java.util.Base64.getDecoder().decode(base64);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"comprobante.pdf\"");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @GetMapping("/cintas")
    public String cintas(@RequestParam(value = "crear", required = false) String crear,
                         org.springframework.ui.Model model, HttpSession session) {
        if (crear != null && !permisoService.puedeCrear(session)) {
            return "redirect:/cintas?denegado";
        }
        java.util.Set<Long> permitidos = permisoService.clientesPermitidos(session);
        model.addAttribute("cintas", cintaDAO.listarTodasCompleto(permitidos));
        model.addAttribute("clientes", permisoService.filtrarClientes(clienteDAO.findAll(), permitidos));
        model.addAttribute("puedeCrear", permisoService.puedeCrear(session));
        return "cintas";
    }

    @PostMapping("/api/crear-cintas")
    @ResponseBody
    public java.util.Map<String, Object> crearCintas(
            @RequestParam("clienteId") int clienteId,
            @RequestParam("tecnologia") String tecnologia,
            @RequestParam("plantilla") String plantilla,
            @RequestParam("numeroInicial") int numeroInicial,
            @RequestParam("cantidad") int cantidad,
            @RequestParam(value = "esLlenable", defaultValue = "true") boolean esLlenable,
            HttpSession session)
    {

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        if (!permisoService.puedeCrear(session)) {
            resp.put("status", "error");
            resp.put("mensaje", "No tenés permiso para crear cintas.");
            return resp;
        }

        int primer = plantilla.indexOf('-');
        int ultimo = plantilla.lastIndexOf('-');
        if (primer < 0) {
            resp.put("status", "error");
            resp.put("mensaje", "La plantilla debe incluir guiones (--) donde va el número.");
            return resp;
        }
        // Los guiones deben ir juntos
        for (int i = primer; i <= ultimo; i++) {
            if (plantilla.charAt(i) != '-') {
                resp.put("status", "error");
                resp.put("mensaje", "Los guiones de la plantilla deben ir juntos.");
                return resp;
            }
        }
        if (cantidad < 1 || cantidad > 500) {
            resp.put("status", "error");
            resp.put("mensaje", "La cantidad debe estar entre 1 y 500.");
            return resp;
        }

        String prefijo = plantilla.substring(0, primer);
        String sufijo = plantilla.substring(ultimo + 1);
        int ancho = ultimo - primer + 1;

        java.util.List<String> barcodes = new java.util.ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            String numStr = String.format("%0" + ancho + "d", numeroInicial + i);
            barcodes.add(prefijo + numStr + sufijo);
        }

        java.util.Map<String, Object> res = cintaDAO.crearCintasLote(barcodes, clienteId, tecnologia, esLlenable);
        resp.put("status", "success");
        resp.putAll(res); // creadas, omitidas
        return resp;
    }
    // ==========================================================
    // DASHBOARD: estadísticas para KPIs y gráficos
    // ==========================================================
    @GetMapping("/api/dashboard-stats")
    @ResponseBody
    public java.util.Map<String, Object> dashboardStats(HttpSession session) {
        java.util.Set<Long> permitidos = permisoService.clientesPermitidos(session);
        java.util.List<Cinta> todas = cintaDAO.filtrarCintas(null, null, permitidos);

        int total = todas.size();
        int libreria = 0, armario = 0, retiradas = 0, virgenes = 0;
        int dSaludable = 0, dAdvertencia = 0, dReemplazar = 0;
        java.util.Map<String, Integer> porUbicacion = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> porCliente = new java.util.HashMap<>();

        for (Cinta c : todas) {
            String ubi = normalizarUbicacion(c.getUbicacion());
            porUbicacion.merge(ubi, 1, Integer::sum);
            if ("Librería".equals(ubi)) libreria++;
            else if ("Armario".equals(ubi)) armario++;
            else if ("Retirado".equals(ubi)) retiradas++;

            if ("PENDIENTE".equalsIgnoreCase(c.getVolumeId())) virgenes++;

            int p = c.getPorcentajeDesgaste();
            if (p >= 100) dReemplazar++;
            else if (p >= 70) dAdvertencia++;
            else dSaludable++;

            String cli = (c.getNombreCliente() != null && !c.getNombreCliente().isEmpty())
                    ? c.getNombreCliente() : "Sin cliente";
            porCliente.merge(cli, 1, Integer::sum);
        }

        java.util.Map<String, Object> kpis = new java.util.HashMap<>();
        kpis.put("total", total);
        kpis.put("libreria", libreria);
        kpis.put("armario", armario);
        kpis.put("retiradas", retiradas);
        kpis.put("virgenes", virgenes);
        kpis.put("reemplazar", dReemplazar);

        java.util.Map<String, Object> desgaste = new java.util.LinkedHashMap<>();
        desgaste.put("saludable", dSaludable);
        desgaste.put("advertencia", dAdvertencia);
        desgaste.put("reemplazar", dReemplazar);

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("kpis", kpis);
        resp.put("desgaste", desgaste);
        resp.put("ubicacion", porUbicacion);
        resp.put("porCliente", porCliente);
        return resp;
    }

    // Agrupa las variantes de ubicación (mayúsculas/tildes) en categorías limpias
    private String normalizarUbicacion(String u) {
        if (u == null || u.trim().isEmpty()) return "Sin ubicación";
        String n = java.text.Normalizer.normalize(u.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase();
        if (n.startsWith("LIBRER")) return "Librería";
        if (n.startsWith("ARMARIO")) return "Armario";
        if (n.startsWith("RETIRAD")) return "Retirado";
        return u.trim();
    }

    // Crea una cinta individual por su barcode exacto
    @PostMapping("/api/crear-cinta-individual")
    @ResponseBody
    public java.util.Map<String, Object> crearCintaIndividual(
            @RequestParam("barcode") String barcode,
            @RequestParam("clienteId") int clienteId,
            @RequestParam("tecnologia") String tecnologia,
            @RequestParam(value = "esLlenable", defaultValue = "true") boolean esLlenable,
            HttpSession session) {

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        if (!permisoService.puedeCrear(session)) {
            resp.put("status", "error");
            resp.put("mensaje", "No tenés permiso para crear cintas.");
            return resp;
        }
        if (barcode == null || barcode.trim().isEmpty()) {
            resp.put("status", "error");
            resp.put("mensaje", "El barcode no puede estar vacío.");
            return resp;
        }
        java.util.List<String> barcodes = java.util.List.of(barcode.trim().toUpperCase());
        java.util.Map<String, Object> res = cintaDAO.crearCintasLote(barcodes, clienteId, tecnologia, esLlenable);
        java.util.List<?> creadas = (java.util.List<?>) res.get("creadas");
        java.util.List<?> omitidas = (java.util.List<?>) res.get("omitidas");
        if (creadas != null && !creadas.isEmpty()) {
            resp.put("status", "success");
            resp.put("mensaje", "Cinta " + barcode.trim().toUpperCase() + " creada correctamente.");
        } else if (omitidas != null && !omitidas.isEmpty()) {
            resp.put("status", "error");
            resp.put("mensaje", "La cinta " + barcode.trim().toUpperCase() + " ya existe.");
        } else {
            resp.put("status", "error");
            resp.put("mensaje", "No se pudo crear la cinta.");
        }
        return resp;
    }

}