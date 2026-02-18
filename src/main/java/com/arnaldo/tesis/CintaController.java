package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.ibm.as400.access.AS400;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CintaController {

    private CintaDAO cintaDAO = new CintaDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    @Autowired
    private LibraryService libraryService;

    @GetMapping("/")
    public String index(@RequestParam(name = "clienteId", required = false) Integer clienteId,
                        @RequestParam(name = "tecnologia", required = false) String tecnologia, Model model) {

        String techFilter = (tecnologia != null && !tecnologia.isEmpty()) ? tecnologia : null;

        // CAMBIO CLAVE: Aquí debe llamar a listarSemanalesManual
        model.addAttribute("cintasSemanales", cintaDAO.listarSemanalesManual(clienteId, techFilter));

        // El inventario completo sigue igual
        model.addAttribute("cintas", cintaDAO.filtrarCintas(clienteId, techFilter));

        model.addAttribute("clientes", clienteDAO.listarTodos());
        model.addAttribute("clienteSeleccionado", clienteId);
        model.addAttribute("tecnologiaSeleccionada", techFilter);

        //consultamos informacion en la librería
        model.addAttribute("cintaEnDrive", libraryService.consultarCintaEnDrive1());
        //consultamos si el robot está trabajando
        model.addAttribute("estadoRobot", libraryService.obtenerEstadoRobot());
        return "index";
    }

    @GetMapping("/desmarcar-semanal/{barcode}")
    public String desmarcar(@PathVariable(name = "barcode") String barcode,
                            @RequestParam(name = "clienteId", required = false) Integer clienteId,
                            @RequestParam(name = "tecnologia", required = false) String tecnologia) {

        cintaDAO.actualizarEstadoSemanal(barcode, false);

        // Retornamos manteniendo los filtros para que no se reinicie la página
        String redirectUrl = "redirect:/?";
        if (clienteId != null) redirectUrl += "clienteId=" + clienteId + "&";
        if (tecnologia != null && !tecnologia.isEmpty()) redirectUrl += "tecnologia=" + tecnologia;

        return redirectUrl;
    }

    @PostMapping("/registrar-cinta")
    public String registrar(@RequestParam(name = "barcode") String barcode,
                            @RequestParam(name = "clienteId") int clienteId,
                            @RequestParam(name = "tecnologia") String tecnologia) { //Recibimos la tecnología del form

        // Ahora pasamos la variable 'tecnologia' real, no el texto fijo "LTO-7"
        cintaDAO.registrarCinta(barcode, "PENDIENTE", clienteId, tecnologia);
        return "redirect:/";
    }

    @GetMapping("/relacionar/{barcode}")
    public String relacionarCinta(@PathVariable(name = "barcode") String barcode) {
        AS400 system = IbmIConnection.getSystem();
        TapeService tapeService = new TapeService();
        String volumeIdEncontrado = tapeService.obtenerVolumeIdDesdeIbmI(system, "TAP07");

        if (volumeIdEncontrado != null) {
            cintaDAO.actualizarVolumeId(barcode, volumeIdEncontrado);
        }
        return "redirect:/";
    }

    @GetMapping("/reiniciar/{barcode}")
    public String reiniciarCinta(@PathVariable(name = "barcode") String barcode) {
        cintaDAO.reiniciarCinta(barcode);
        return "redirect:/";
    }

    //seccion donde seleccionamos la cintas semanales
    @GetMapping("/marcar-semanal/{barcode}")
    public String marcar(@PathVariable(name = "barcode") String barcode,
                         @RequestParam(name = "clienteId", required = false) Integer clienteId,
                         @RequestParam(name = "tecnologia", required = false) String tecnologia) {
        cintaDAO.actualizarEstadoSemanal(barcode, true);
        // Construimos la URL de regreso con los filtros actuales
        String params = construirParametros(clienteId, tecnologia);
        return "redirect:/" + params;
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

}