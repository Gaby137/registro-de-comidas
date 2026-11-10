package com.comidas.util;

import java.security.SecureRandom;

/**
 * Generador de contraseñas temporales seguras.
 */
public final class PasswordGenerator {

    private PasswordGenerator() {}

    private static final String MAYUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMEROS = "0123456789";
    private static final String ESPECIALES = "!@#$%&*";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS + ESPECIALES;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera una contraseña aleatoria de la longitud especificada.
     * Garantiza al menos una mayúscula, una minúscula, un número y un especial.
     */
    public static String generar(int longitud) {
        if (longitud < 8) {
            throw new IllegalArgumentException("La longitud mínima es 8 caracteres");
        }

        StringBuilder password = new StringBuilder(longitud);

        // Garantizar al menos un carácter de cada tipo
        password.append(MAYUSCULAS.charAt(RANDOM.nextInt(MAYUSCULAS.length())));
        password.append(MINUSCULAS.charAt(RANDOM.nextInt(MINUSCULAS.length())));
        password.append(NUMEROS.charAt(RANDOM.nextInt(NUMEROS.length())));
        password.append(ESPECIALES.charAt(RANDOM.nextInt(ESPECIALES.length())));

        // Completar con caracteres aleatorios
        for (int i = 4; i < longitud; i++) {
            password.append(TODOS.charAt(RANDOM.nextInt(TODOS.length())));
        }

        // Mezclar para que los caracteres garantizados no estén siempre al inicio
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    /**
     * Genera una contraseña de 12 caracteres por defecto.
     */
    public static String generar() {
        return generar(12);
    }
}
