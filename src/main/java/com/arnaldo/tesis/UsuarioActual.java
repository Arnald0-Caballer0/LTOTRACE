package com.arnaldo.tesis;

/**
 * Guarda el usuario que está haciendo la petición actual, para que
 * cualquier parte del código (como el CintaDAO) pueda saber "quién" sin
 * tener que recibirlo por parámetro en cada método.
 *
 * Es un "casillero" por hilo (ThreadLocal): cada petición HTTP corre en su
 * propio hilo, así que cada una tiene su propio usuario sin mezclarse con otras.
 *
 * IMPORTANTE: el interceptor lo llena al inicio de la petición y lo limpia
 * al final, para no dejar datos viejos en el hilo (que se reutiliza).
 */
public class UsuarioActual {

    private static final ThreadLocal<String> USUARIO = new ThreadLocal<>();

    public static void set(String username) {
        USUARIO.set(username);
    }

    /** Devuelve el usuario actual, o "sistema" si no hay ninguno (ej. tareas internas). */
    public static String get() {
        String u = USUARIO.get();
        return (u != null && !u.isBlank()) ? u : "sistema";
    }

    public static void limpiar() {
        USUARIO.remove();
    }
}
