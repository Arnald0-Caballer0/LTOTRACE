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

import javax.net.ssl.SSLContext;
import java.util.Map;

@Service
public class LibraryService {

    @Value("${library.ts4300.ip}")
    private String ip;

    @Value("${library.ts4300.user}")
    private String user;

    @Value("${library.ts4300.password}")
    private String pass;

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
        // El login sigue siendo v1 según el estándar de IBM
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

                    if ("1".equals(logicalNum)) {
                        // Extraemos el Barcode de la cinta
                        JsonNode barcodeNode = drive.get("Barcode");
                        if (barcodeNode != null && !barcodeNode.isNull()) {
                            return barcodeNode.asText();
                        } else {
                            return "DRIVE_VACÍO";
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
}