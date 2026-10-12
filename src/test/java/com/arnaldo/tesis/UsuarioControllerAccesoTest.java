package com.arnaldo.tesis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CP-05 — Verificación del principio de mínimo privilegio.
 * Un usuario con rol OPERADOR (o sin sesión) que intenta acceder por URL
 * directa a un módulo administrativo debe ser redirigido, no ingresar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CP-05 · Acceso directo por URL a módulo admin (MockMvc)")
class UsuarioControllerAccesoTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthService authService;
    @Mock private AuditoriaService auditoria;
    @Mock private RolRepository rolRepository;
    @Mock private ClienteDAO clienteDAO;

    @InjectMocks
    private UsuarioController usuarioController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController).build();
    }

    private Rol rol(String codigo) {
        Rol r = new Rol();
        r.setCodigo(codigo);
        r.setNombre(codigo);
        return r;
    }

    @Test
    @DisplayName("Operador que accede a /usuarios es redirigido a /?denegado")
    void operador_esRedirigidoADenegado() throws Exception {
        Usuario operador = new Usuario();
        operador.setUsername("operador");
        operador.setRol(rol("OPERADOR"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginController.SESSION_USER, operador);

        mockMvc.perform(get("/usuarios").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?denegado"));
    }

    @Test
    @DisplayName("Acceso a /usuarios sin sesión iniciada también es redirigido")
    void sinSesion_esRedirigidoADenegado() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?denegado"));
    }
}
