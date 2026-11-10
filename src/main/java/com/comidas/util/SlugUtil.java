package com.comidas.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Generador de slugs para URLs amigables.
 * <ul>
 *   <li>"Sweater Cuello Tortuga" → "sweater-cuello-tortuga"</li>
 *   <li>"Ñandú & Café 100%" → "nandu-cafe-100"</li>
 *   <li>"Remera / Buzo -- Talle M" → "remera-buzo-talle-m"</li>
 * </ul>
 */
public final class SlugUtil {

    private SlugUtil() {}

    private static final Pattern ACENTOS = Pattern.compile("\\p{M}");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");
    private static final Pattern GUIONES_EN_BORDES = Pattern.compile("(^-+|-+$)");

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String sinAcentos = ACENTOS.matcher(Normalizer.normalize(input, Normalizer.Form.NFD)).replaceAll("");
        String slug = NO_ALFANUMERICO.matcher(sinAcentos.toLowerCase(Locale.ROOT)).replaceAll("-");
        return GUIONES_EN_BORDES.matcher(slug).replaceAll("");
    }
}
