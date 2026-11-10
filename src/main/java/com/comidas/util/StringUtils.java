package com.comidas.util;

/**
 * Utilidades para manejo de strings.
 */
public final class StringUtils {

    private StringUtils() {}

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * Devuelve el string o un valor por defecto si es null/vacío.
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isNullOrEmpty(str) ? defaultValue : str;
    }

    /**
     * Capitaliza la primera letra: "hola mundo" → "Hola mundo"
     */
    public static String capitalize(String str) {
        if (isNullOrEmpty(str)) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Trunca un string a un máximo de caracteres, agregando "..." si se cortó.
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Limpia un string removiendo espacios extra y caracteres de control.
     */
    public static String clean(String str) {
        if (str == null) return null;
        return str.strip().replaceAll("\\s+", " ");
    }
}
