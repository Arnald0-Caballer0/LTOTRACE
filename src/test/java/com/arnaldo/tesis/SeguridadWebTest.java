package com.arnaldo.tesis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verificación de la configuración de Spring Security (RNF-01):
 * protección CSRF y cabeceras de seguridad HTTP.
 *
 * Se carga solo SecurityConfig con un controlador de prueba, sin base de datos
 * ni conexiones externas.
 */
@SpringJUnitWebConfig(SeguridadWebTest.Config.class)
@DisplayName("Seguridad web · Spring Security: protección CSRF y cabeceras HTTP")
class SeguridadWebTest {

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class Config {
        @Bean
        PruebaController pruebaController() {
            return new PruebaController();
        }
    }

    /** Controlador mínimo que solo responde "ok". */
    @RestController
    static class PruebaController {
        @GetMapping("/api/prueba")
        String consultar() { return "ok"; }

        @PostMapping("/api/prueba")
        String modificar() { return "ok"; }

        @PostMapping("/formulario")
        String enviarFormulario() { return "ok"; }
    }

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private MockHttpSession sesionIniciada() {
        Usuario u = new Usuario();
        u.setUsername("operador");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginController.SESSION_USER, u);
        return session;
    }

    @Test
    @DisplayName("POST con sesión y sin token CSRF es rechazado (403)")
    void postSinToken_esRechazado() throws Exception {
        mockMvc.perform(post("/api/prueba").session(sesionIniciada()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-LTO-Motivo", "csrf"));
    }

    @Test
    @DisplayName("POST con sesión y token CSRF válido es aceptado (200)")
    void postConToken_esAceptado() throws Exception {
        mockMvc.perform(post("/api/prueba").session(sesionIniciada()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST con token CSRF inválido es rechazado (403)")
    void postConTokenInvalido_esRechazado() throws Exception {
        mockMvc.perform(post("/api/prueba").session(sesionIniciada()).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST a la API sin sesión responde 401 (sesión expirada)")
    void postApiSinSesion_respondeSesionExpirada() throws Exception {
        mockMvc.perform(post("/api/prueba"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Formulario enviado sin sesión redirige al login")
    void formularioSinSesion_redirigeAlLogin() throws Exception {
        mockMvc.perform(post("/formulario"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expirada"));
    }

    @Test
    @DisplayName("Las consultas (GET) no requieren token")
    void getSinToken_esAceptado() throws Exception {
        mockMvc.perform(get("/api/prueba").session(sesionIniciada()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Las respuestas incluyen las cabeceras de seguridad HTTP")
    void respuestas_incluyenCabecerasDeSeguridad() throws Exception {
        mockMvc.perform(get("/api/prueba").secure(true))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Content-Security-Policy", "frame-ancestors 'none'"))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }
}
