package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Motor de conciliación de inventario: compara el inventario físico de la
 * librería TS4300 contra el inventario registrado en PostgreSQL y detecta
 * inconsistencias, persistiendo el resultado.
 */
@Service
public class ConciliacionService {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private AuditoriaInventarioRepository auditoriaRepo;

    private final CintaDAO cintaDAO = new CintaDAO();

    public AuditoriaInventario ejecutar(String usuario) {
        Map<String, String> fisico = libraryService.obtenerInventarioFisico();

        // Si no se pudo obtener el inventario físico (TS4300 no disponible),
        // se aborta la auditoría en vez de marcar todo como "no localizado".
        if (fisico == null) {
            throw new IllegalStateException(
                    "No se pudo conectar con la TS4300 para obtener el inventario físico. " +
                    "Verifique la conexión e intente nuevamente.");
        }

        List<Map<String, Object>> registradas = cintaDAO.listarTodasCompleto(null);

        AuditoriaInventario aud = new AuditoriaInventario();
        aud.setFecha(LocalDateTime.now());
        aud.setUsuario(usuario);

        Set<String> barcodesRegistrados = new HashSet<>();
        Set<String> conProblema = new HashSet<>();   // medios DISTINTOS con al menos una inconsistencia

        // Conteo de Volume IDs para detectar duplicados
        Map<String, Integer> volIdCount = new HashMap<>();
        for (Map<String, Object> c : registradas) {
            String vol = str(c.get("volumeId"));
            if (!vol.isEmpty() && !"PENDIENTE".equalsIgnoreCase(vol)) {
                volIdCount.merge(vol.toUpperCase(), 1, Integer::sum);
            }
        }

        for (Map<String, Object> c : registradas) {
            String barcode = str(c.get("barcode")).toUpperCase();
            barcodesRegistrados.add(barcode);
            String ubi = normalizarUbicacion(str(c.get("ubicacion")));
            String vol = str(c.get("volumeId"));
            boolean presente = fisico.containsKey(barcode);
            String ubiFisica = presente ? fisico.get(barcode) : "\u2014";

            // Reglas de ubicación (según lo físico vs lo registrado)
            if ("LIBRERIA".equals(ubi) && !presente) {
                aud.agregarDetalle(new AuditoriaInventarioDetalle(barcode, "NO_LOCALIZADA",
                        "Registrada en Librería pero no está físicamente en la TS4300.", ubi, ubiFisica));
                conProblema.add(barcode);
            } else if ("ARMARIO".equals(ubi) && presente) {
                aud.agregarDetalle(new AuditoriaInventarioDetalle(barcode, "UBICACION_INCORRECTA",
                        "Registrada en Armario pero está físicamente en la librería.", ubi, ubiFisica));
                conProblema.add(barcode);
            } else if ("RETIRADO".equals(ubi) && presente) {
                aud.agregarDetalle(new AuditoriaInventarioDetalle(barcode, "RETIRADA_EN_LIBRERIA",
                        "Registrada como Retirada pero sigue en la librería.", ubi, ubiFisica));
                conProblema.add(barcode);
            }

            // Trazabilidad (Volume ID)
            if (vol.isEmpty() || "PENDIENTE".equalsIgnoreCase(vol)) {
                aud.agregarDetalle(new AuditoriaInventarioDetalle(barcode, "SIN_VOLUME_ID",
                        "Trazabilidad incompleta: la cinta no tiene Volume ID asignado.", ubi, ubiFisica));
                conProblema.add(barcode);
            } else if (volIdCount.getOrDefault(vol.toUpperCase(), 0) > 1) {
                aud.agregarDetalle(new AuditoriaInventarioDetalle(barcode, "VOLUME_ID_DUPLICADO",
                        "El Volume ID " + vol + " está asignado a más de una cinta.", ubi, ubiFisica));
                conProblema.add(barcode);
            }
        }

        // Cintas físicas que no están registradas (también son medios con inconsistencia)
        for (Map.Entry<String, String> e : fisico.entrySet()) {
            if (!barcodesRegistrados.contains(e.getKey())) {
                aud.agregarDetalle(new AuditoriaInventarioDetalle(e.getKey(), "DESCONOCIDA",
                        "Cinta presente en la librería pero no registrada en el sistema.", "\u2014", e.getValue()));
                conProblema.add(e.getKey());
            }
        }

        // Totales coherentes: coincidencias + inconsistencias = analizadas
        Set<String> universo = new HashSet<>(barcodesRegistrados);
        universo.addAll(fisico.keySet());
        aud.setTotalAnalizadas(universo.size());
        aud.setTotalInconsistencias(conProblema.size());              // medios distintos con problemas
        aud.setCoincidencias(universo.size() - conProblema.size());   // medios sin ningún problema

        return auditoriaRepo.save(aud);
    }

    private String str(Object o) { return o == null ? "" : o.toString().trim(); }

    private String normalizarUbicacion(String u) {
        if (u == null) return "";
        String n = java.text.Normalizer.normalize(u.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase();
        if (n.startsWith("LIBRER")) return "LIBRERIA";
        if (n.startsWith("ARMARIO")) return "ARMARIO";
        if (n.startsWith("RETIRAD")) return "RETIRADO";
        return n;
    }
}
