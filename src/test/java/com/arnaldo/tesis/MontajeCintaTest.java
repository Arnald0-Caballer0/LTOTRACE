package com.arnaldo.tesis;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP-12 (complemento) — Montaje con el robot y registro del uso (RF-04 y RF-08).
 * La librería TS4300 y la base de datos se sustituyen por dobles de prueba.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CP-12 · Montaje de cintas y registro del uso")
class MontajeCintaTest {

    @Mock private LibraryService libraryService;
    @Mock private PermisoService permisoService;
    @Mock private CintaDAO cintaDAO;
    @Mock private HttpSession session;

    private CintaController controller;

    @BeforeEach
    void preparar() {
        controller = new CintaController();
        ReflectionTestUtils.setField(controller, "libraryService", libraryService);
        ReflectionTestUtils.setField(controller, "permisoService", permisoService);
        ReflectionTestUtils.setField(controller, "cintaDAO", cintaDAO);
    }

    private void usuarioConPermisoDeMover() {
        when(permisoService.puedeMover(session)).thenReturn(true);
        when(permisoService.clientesPermitidos(session)).thenReturn(null); // alcance sobre todos los clientes
    }

    @Test
    @DisplayName("Al montar la cinta en el drive se suma un uso y se registra MONTAJE")
    void montajeSumaUnUso() {
        usuarioConPermisoDeMover();
        when(libraryService.moverCinta("CC0127L8")).thenReturn("SUCCESS_LOAD");

        Map<String, String> r = controller.moverCintaRobot("CC0127L8", session);

        assertThat(r.get("status")).isEqualTo("success");
        verify(cintaDAO).registrarMontaje("CC0127L8");
    }

    @Test
    @DisplayName("Al desmontar la cinta no se suma un uso: solo se registra DESMONTAJE")
    void desmontajeNoSumaUso() {
        usuarioConPermisoDeMover();
        when(libraryService.moverCinta("CC0127L8")).thenReturn("SUCCESS_UNLOAD");

        Map<String, String> r = controller.moverCintaRobot("CC0127L8", session);

        assertThat(r.get("status")).isEqualTo("success");
        verify(cintaDAO, never()).registrarMontaje(anyString());
        verify(cintaDAO).registrarMovimiento("CC0127L8", "DESMONTAJE", "Cinta descargada del drive TS4300");
    }

    @Test
    @DisplayName("Si el robot falla no se registra ningún uso")
    void falloDelRobotNoRegistraUso() {
        usuarioConPermisoDeMover();
        when(libraryService.moverCinta("CC0127L8")).thenReturn("Error inesperado del robot al intentar mover la cinta. Código HTTP: 500");

        Map<String, String> r = controller.moverCintaRobot("CC0127L8", session);

        assertThat(r.get("status")).isEqualTo("error");
        verify(cintaDAO, never()).registrarMontaje(anyString());
    }

    @Test
    @DisplayName("Un usuario sin permiso de mover no puede montar cintas")
    void sinPermisoNoMueve() {
        when(permisoService.puedeMover(session)).thenReturn(false);

        Map<String, String> r = controller.moverCintaRobot("CC0127L8", session);

        assertThat(r.get("status")).isEqualTo("error");
        verify(libraryService, never()).moverCinta(anyString());
        verify(cintaDAO, never()).registrarMontaje(anyString());
    }
}
