package com.arnaldo.tesis;

/**
 * Resultado de un intento de inicio de sesión, con el estado y datos asociados.
 */
public class ResultadoLogin {

    public enum Estado {
        EXITO,
        CREDENCIALES_INVALIDAS,
        BLOQUEADO_TEMPORAL,
        DESHABILITADO
    }

    private final Estado estado;
    private final Usuario usuario;
    private final long minutosRestantes;

    private ResultadoLogin(Estado estado, Usuario usuario, long minutosRestantes) {
        this.estado = estado;
        this.usuario = usuario;
        this.minutosRestantes = minutosRestantes;
    }

    public Estado getEstado() { return estado; }
    public Usuario getUsuario() { return usuario; }
    public long getMinutosRestantes() { return minutosRestantes; }

    public static ResultadoLogin exito(Usuario u) { return new ResultadoLogin(Estado.EXITO, u, 0); }
    public static ResultadoLogin credencialesInvalidas() { return new ResultadoLogin(Estado.CREDENCIALES_INVALIDAS, null, 0); }
    public static ResultadoLogin bloqueadoTemporal(long minutos) { return new ResultadoLogin(Estado.BLOQUEADO_TEMPORAL, null, minutos); }
    public static ResultadoLogin deshabilitado() { return new ResultadoLogin(Estado.DESHABILITADO, null, 0); }
}
