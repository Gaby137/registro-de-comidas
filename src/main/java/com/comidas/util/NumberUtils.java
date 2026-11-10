package com.comidas.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utilidades numéricas con formato argentino (1.234,56).
 */
public final class NumberUtils {

    private NumberUtils() {}

    // DecimalFormat no es thread-safe: un formatter por thread (las requests web corren en paralelo)
    private static final ThreadLocal<DecimalFormat> FORMATO_MONEDA =
            ThreadLocal.withInitial(() -> crearFormato("#,##0.00"));
    private static final ThreadLocal<DecimalFormat> FORMATO_DECIMAL =
            ThreadLocal.withInitial(() -> crearFormato("#,##0.##"));

    private static DecimalFormat crearFormato(String patron) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "AR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return new DecimalFormat(patron, symbols);
    }

    /**
     * Formatea un BigDecimal como moneda argentina: "45.000,00"
     */
    public static String formatMoneda(BigDecimal valor) {
        if (valor == null) return "0,00";
        return FORMATO_MONEDA.get().format(valor);
    }

    /**
     * Formatea un número con decimales opcionales: "1.500,5" o "1.500"
     */
    public static String formatDecimal(BigDecimal valor) {
        if (valor == null) return "0";
        return FORMATO_DECIMAL.get().format(valor);
    }

    /**
     * Redondea a N decimales con HALF_UP.
     */
    public static BigDecimal redondear(BigDecimal valor, int decimales) {
        if (valor == null) return BigDecimal.ZERO;
        return valor.setScale(decimales, RoundingMode.HALF_UP);
    }

    /**
     * Verifica si un BigDecimal es positivo (mayor a cero).
     */
    public static boolean esPositivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Verifica si un BigDecimal es cero o null.
     */
    public static boolean esCeroONull(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Parsea un número en formato argentino ("1.500,50" → 1500.50) de forma segura.
     * Retorna BigDecimal.ZERO si el string es null, vacío o inválido.
     * <p>
     * Ojo: el punto se toma como separador de miles ("1.5" → 15). Para valores que ya vienen
     * con punto decimal (ej: un input type="number" o JSON), usá new BigDecimal(valor).
     */
    public static BigDecimal parseSafe(String valor) {
        if (valor == null || valor.isBlank()) return BigDecimal.ZERO;
        try {
            String limpio = valor.replace(".", "").replace(",", ".");
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
