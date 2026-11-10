package com.comidas.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Las fechas se guardan en UTC y se muestran siempre en hora de Argentina,
 * sin depender de la zona horaria de la JVM ni del navegador.
 */
public final class ZonaHoraria {

    private ZonaHoraria() {}

    public static final ZoneId AR = ZoneId.of("America/Argentina/Buenos_Aires");
    public static final Locale ES = Locale.forLanguageTag("es-AR");

    public static LocalDateTime local(Instant instante) {
        return LocalDateTime.ofInstant(instante, AR);
    }

    public static Instant instante(LocalDateTime local) {
        return local.atZone(AR).toInstant();
    }

    public static Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(AR).toInstant();
    }

    public static LocalDate hoy(Clock clock) {
        return LocalDate.ofInstant(clock.instant(), AR);
    }
}
