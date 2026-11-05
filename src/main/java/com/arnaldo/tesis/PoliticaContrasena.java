package com.arnaldo.tesis;

/**
 * Política mínima de contraseñas de LTOTRACE.
 * Se aplica al crear un usuario y al restablecer su contraseña.
 *
 *  - Entre 8 y 72 caracteres (72 es el límite que procesa BCrypt).
 *  - Al menos una letra y al menos un número.
 *  - No puede contener el nombre de usuario.
 */
public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 72;

    private PoliticaContrasena() {}

    /**
     * Valida la contraseña propuesta.
     * @return null si cumple la política; en caso contrario, el motivo del rechazo.
     */
    public static String validar(String password, String username) {
        if (password == null || password.isBlank()) {
            return "La contraseña es obligatoria.";
        }
        if (password.length() < LONGITUD_MINIMA) {
            return "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.";
        }
        if (password.length() > LONGITUD_MAXIMA) {
            return "La contraseña no puede superar los " + LONGITUD_MAXIMA + " caracteres.";
        }
        boolean tieneLetra = password.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = password.chars().anyMatch(Character::isDigit);
        if (!tieneLetra || !tieneNumero) {
            return "La contraseña debe combinar letras y números.";
        }
        if (username != null && username.trim().length() >= 3
                && password.toLowerCase().contains(username.trim().toLowerCase())) {
            return "La contraseña no puede contener el nombre de usuario.";
        }
        return null;
    }

    public static boolean cumple(String password, String username) {
        return validar(password, username) == null;
    }
}
