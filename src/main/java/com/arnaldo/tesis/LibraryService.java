package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import java.util.Map;

@Service
public class LibraryService {

    private String ip;
    private String user;
    private String pass;

    @org.springframework.beans.factory.annotation.Autowired
    private ConfiguracionService cfg;

    private final RestTemplate restTemplate;

    public LibraryService() throws Exception {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();

        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build())
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();

        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    private String obtenerToken() {
        // Cargamos la conexión desde la configuración cifrada
        ip = cfg.get("ts4300.ip");
        user = cfg.get("ts4300.usuario");
        pass = cfg.getSecreto("ts4300.password");

        String url = "https://" + ip + ":3031/v1/login";
        Map<String, String> loginData = Map.of("username", user, "password", pass);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, loginData, Map.class);
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                return response.getBody().get("token").toString();
            }
        } catch (Exception e) {
            System.err.println("Error de Auth: " + e.getMessage());
        }
        return null;
    }

    public String consultarCintaEnDrive1() {
        String token = obtenerToken();
        if (token == null) return "ERROR_AUTH";

        // NUEVO ENDPOINT DESCUBIERTO EN POSTMAN
        String url = "https://" + ip + ":3031/rest/drive/information";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String jsonBody = response.getBody();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonBody);

            // El JSON es un Array [ {...}, {...} ]
            if (root.isArray()) {
                for (JsonNode drive : root) {
                    // Buscamos LogicalNumber como String "1"
                    String logicalNum = drive.has("LogicalNumber") ? drive.get("LogicalNumber").asText() : "";

                    if ("3".equals(logicalNum)) {
                        // Extraemos el Barcode de la cinta
                        JsonNode barcodeNode = drive.get("Barcode");
                        if (barcodeNode != null && !barcodeNode.isNull()) {
                            return barcodeNode.asText();
                        } else {
                            return "VACÍO";
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando el JSON de Postman: " + e.getMessage());
        }
        return "SIN_DATOS";
    }

    public String obtenerEstadoRobot() {
        String token = obtenerToken();
        if (token == null) return "OFFLINE";

        String url = "https://" + ip + ":3031/rest/library/status";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode root = response.getBody();

            if (root != null && root.has("BaseStatus")) {
                return root.get("BaseStatus").get("RobStatus").asText();
            }
        } catch (Exception e) {
            System.err.println("Error consultando estado del robot: " + e.getMessage());
        }
        return "DESCONOCIDO";
    }

    // ==========================================
    // MÉTODOS AUXILIARES PARA LEER EL JSON
    // ==========================================

    private String buscarSlotDeCinta(JsonNode slotsNode, String barcode) {
        if (slotsNode != null && slotsNode.isArray()) {
            for (JsonNode slot : slotsNode) {
                // Buscamos un slot que tenga Cartridge = TRUE y el Barcode coincida
                if ("TRUE".equalsIgnoreCase(slot.path("Cartridge").asText()) &&
                        barcode.equalsIgnoreCase(slot.path("Barcode").asText())) {
                    return slot.path("LogicalNumber").asText();
                }
            }
        }
        return null; // No se encontró la cinta
    }

    private String buscarSlotVacio(JsonNode slotsNode) {
        if (slotsNode != null && slotsNode.isArray()) {
            for (JsonNode slot : slotsNode) {
                // Buscamos un slot donde Cartridge sea FALSE
                if ("FALSE".equalsIgnoreCase(slot.path("Cartridge").asText())) {
                    String logicalNumber = slot.path("LogicalNumber").asText();
                    // Asegurarnos de que sea un slot válido de la forma "1.X" (del 1.1 al 1.40)
                    if (logicalNumber != null && logicalNumber.startsWith("1.")) {
                        return logicalNumber;
                    }
                }
            }
        }
        return null; // Si llega aquí, es que los 40 slots están llenos
    }

    // Método principal que será llamado desde el botón de la web
    public String moverCinta(String barcode) {
        try {
            // 1. OBTENER TOKEN DE AUTENTICACIÓN
            String token = obtenerToken();
            if (token == null) return "Error: No se pudo autenticar con la TS4300.";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. OBTENER INVENTARIO (Usando tu RestTemplate)
            String urlInventario = "https://" + ip + ":3031/rest/partition/inventory?partitionNum=\" + cfg.get(\"ts4300.particion\")";
            HttpEntity<String> entityGet = new HttpEntity<>(headers);
            ResponseEntity<String> responseInv = restTemplate.exchange(urlInventario, HttpMethod.GET, entityGet, String.class);
            String inventarioJson = responseInv.getBody();

            // Analizar el JSON con Jackson
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(inventarioJson);
            JsonNode slotsNode = rootNode.path("Slots");

            // 3. LÓGICA DE MOVIMIENTO
            String cintaActualEnDrive = consultarCintaEnDrive1();
            boolean driveVacio = (cintaActualEnDrive == null || cintaActualEnDrive.trim().isEmpty() || cintaActualEnDrive.equals("---") || cintaActualEnDrive.equals("VACÍO"));
            boolean esLaMismaCinta = barcode.equalsIgnoreCase(cintaActualEnDrive);

            String jsonPayload = "";
            String driveAddress = cfg.get("ts4300.drive");
            String accion = ""; // LOAD (carga = uso) o UNLOAD (descarga)

            if (esLaMismaCinta) {
                // ==========================================
                // ACCIÓN: DESCARGAR (Drive -> Slot)
                // ==========================================
                accion = "UNLOAD";
                String slotVacio = buscarSlotVacio(slotsNode);
                if (slotVacio == null) {
                    return "Error: La librería está llena. No hay slots vacíos (1.1 - 1.40) para descargar la cinta.";
                }

                jsonPayload = "{" +
                        "\"SrcType\": \"Drive\"," +
                        "\"SrcAddress\": \"" + driveAddress + "\"," +
                        "\"DestType\": \"Slot\"," +
                        "\"DestAddress\": \"" + slotVacio + "\"" +
                        "}";
            } else {
                // ==========================================
                // ACCIÓN: CARGAR (Slot -> Drive) = un USO
                // ==========================================
                accion = "LOAD";
                // SEGURIDAD: Si el Drive tiene OTRA cinta, bloqueamos.
                if (!driveVacio) {
                    return "Error de Seguridad: El Drive ya está ocupado por la cinta [" + cintaActualEnDrive + "]. Debes descargarla primero.";
                }

                String slotOrigen = buscarSlotDeCinta(slotsNode, barcode);
                if (slotOrigen == null) {
                    return "Error: No se encontró la cinta [" + barcode + "] en los slots de la librería.";
                }

                jsonPayload = "{" +
                        "\"SrcType\": \"Slot\"," +
                        "\"SrcAddress\": \"" + slotOrigen + "\"," +
                        "\"DestType\": \"Drive\"," +
                        "\"DestAddress\": \"" + driveAddress + "\"" +
                        "}";
            }

            // 4. ENVIAR LA ORDEN DE MOVIMIENTO (POST)
            String urlMove = "https://" + ip + ":3031/rest/library/movemedia";
            HttpEntity<String> entityPost = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> responseMove = restTemplate.exchange(urlMove, HttpMethod.POST, entityPost, String.class);

            if (responseMove.getStatusCode() == HttpStatus.OK || responseMove.getStatusCode() == HttpStatus.CREATED || responseMove.getStatusCode() == HttpStatus.ACCEPTED) {
                // Devolvemos qué acción fue: SUCCESS_LOAD (uso) o SUCCESS_UNLOAD (descarga)
                return "SUCCESS_" + accion;
            } else {
                return "Error inesperado del robot al intentar mover la cinta. Código HTTP: " + responseMove.getStatusCodeValue();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Excepción interna al comunicarse con TS4300: " + e.getMessage();
        }
    }
    // Prueba la conexión a la TS4300 con los datos guardados (para el botón "Probar conexión")
    public boolean probarConexion() {
        try {
            return obtenerToken() != null;
        } catch (Exception e) {
            System.err.println("Error probando TS4300: " + e.getMessage());
            return false;
        }
    }

}