package com.arnaldo.tesis;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * CP-06 — Permisos operativos y alcance de clientes.
 * Un usuario sin la capacidad correspondiente no puede operar, y un usuario
 * con alcance limitado solo ve los clientes asignados. El ADMIN siempre puede.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CP-06 · PermisoService: capacidades y alcance de clientes")
class PermisoServiceTest {

    @Mock
    private HttpSession session;

    private final PermisoService permisoService = new PermisoService();

    // ---------- Helpers para construir usuarios de prueba ----------

    private Rol rol(String codigo) {
        Rol r = new Rol();
        r.setCodigo(codigo);
        r.setNombre(codigo);
        return r;
    }

    private Cliente cliente(long id, String nombre) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombre(nombre);
        return c;
    }

    private void enSesion(Usuario u) {
        when(session.getAttribute(LoginController.SESSION_USER)).thenReturn(u);
    }

    // ---------- Alcance de clientes ----------

    @Test
    @DisplayName("El ADMIN no tiene restricción de clientes (ve todos)")
    void admin_veTodosLosClientes() {
        Usuario admin = new Usuario();
        admin.setRol(rol("ADMIN"));
        admin.setVeTodosClientes(false); // aunque diga false, el ADMIN ignora la restricción
        enSesion(admin);

        assertThat(permisoService.clientesPermitidos(session)).isNull(); // null = sin filtro
        assertThat(permisoService.puedeVerCliente(session, 99L)).isTrue();
    }

    @Test
    @DisplayName("Operador con 've todos los clientes' no tiene restricción")
    void operadorVeTodos_sinRestriccion() {
        Usuario u = new Usuario();
        u.setRol(rol("OPERADOR"));
        u.setVeTodosClientes(true);
        enSesion(u);

        assertThat(permisoService.clientesPermitidos(session)).isNull();
    }

    @Test
    @DisplayName("Operador restringido solo ve sus clientes asignados")
    void operadorRestringido_soloVeAsignados() {
        Usuario u = new Usuario();
        u.setRol(rol("OPERADOR"));
        u.setVeTodosClientes(false);
        u.setClientesAsignados(Set.of(cliente(1L, "Tu Financiera")));
        enSesion(u);

        Set<Long> permitidos = permisoService.clientesPermitidos(session);
        assertThat(permitidos).containsExactly(1L);
        assertThat(permisoService.puedeVerCliente(session, 1L)).isTrue();
        assertThat(permisoService.puedeVerCliente(session, 2L)).isFalse(); // cliente no asignado
    }

    @Test
    @DisplayName("filtrarClientes deja solo los permitidos para un operador restringido")
    void filtrarClientes_dejaSoloPermitidos() {
        List<Cliente> todos = List.of(cliente(1L, "A"), cliente(2L, "B"), cliente(3L, "C"));

        // Restringido a los clientes 1 y 3
        List<Cliente> filtrados = permisoService.filtrarClientes(todos, Set.of(1L, 3L));
        assertThat(filtrados).extracting(Cliente::getId).containsExactlyInAnyOrder(1L, 3L);

        // Sin restricción (null) -> devuelve todos
        assertThat(permisoService.filtrarClientes(todos, null)).hasSize(3);
    }

    // ---------- Capacidades (mover / crear / retirar) ----------

    @Test
    @DisplayName("Operador sin permiso de retirar no puede retirar; el ADMIN sí puede todo")
    void capacidades_seRespetan() {
        // Operador con retiro deshabilitado, pero mover habilitado
        Usuario op = new Usuario();
        op.setRol(rol("OPERADOR"));
        op.setPuedeMoverCintas(true);
        op.setPuedeCrearCintas(false);
        op.setPuedeRetirarCintas(false);
        enSesion(op);

        assertThat(permisoService.puedeMover(session)).isTrue();
        assertThat(permisoService.puedeCrear(session)).isFalse();
        assertThat(permisoService.puedeRetirar(session)).isFalse();
    }

    @Test
    @DisplayName("El ADMIN puede mover, crear y retirar aunque tenga los flags en false")
    void admin_puedeTodasLasCapacidades() {
        Usuario admin = new Usuario();
        admin.setRol(rol("ADMIN"));
        admin.setPuedeMoverCintas(false);
        admin.setPuedeCrearCintas(false);
        admin.setPuedeRetirarCintas(false);
        enSesion(admin);

        assertThat(permisoService.puedeMover(session)).isTrue();
        assertThat(permisoService.puedeCrear(session)).isTrue();
        assertThat(permisoService.puedeRetirar(session)).isTrue();
    }
}
