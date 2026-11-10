package com.comidas.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utilidades para fechas con la API java.time (sin java.util.Date ni Calendar).
 */
public final class DateUtils {

    private DateUtils() {}

    // === Formatters comunes ===
    public static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DD_MM_YYYY_GUION = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DD_MM_YYYY_HH_MM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter ANSI = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter MES_ANIO = DateTimeFormatter.ofPattern("MM/yyyy");

    // === Format ===

    public static String format(LocalDate date) {
        return format(date, DD_MM_YYYY);
    }

    public static String format(LocalDate date, DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : "";
    }

    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DD_MM_YYYY_HH_MM);
    }

    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime != null ? dateTime.format(formatter) : "";
    }

    public static String format(LocalTime time) {
        return format(time, HH_MM);
    }

    public static String format(LocalTime time, DateTimeFormatter formatter) {
        return time != null ? time.format(formatter) : "";
    }

    // === Parse ===

    public static LocalDate parseDate(String date) {
        return LocalDate.parse(date, DD_MM_YYYY);
    }

    public static LocalDate parseDateISO(String date) {
        return LocalDate.parse(date, YYYY_MM_DD);
    }

    public static LocalDateTime parseDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DD_MM_YYYY_HH_MM);
    }

    public static LocalTime parseTime(String time) {
        return LocalTime.parse(time, HH_MM);
    }

    // === Utilidades ===

    public static LocalDate primerDiaMes(int mes, int anio) {
        return LocalDate.of(anio, mes, 1);
    }

    public static LocalDate ultimoDiaMes(int mes, int anio) {
        LocalDate fecha = LocalDate.of(anio, mes, 1);
        return fecha.withDayOfMonth(fecha.lengthOfMonth());
    }

    public static LocalDate primerDiaMes(LocalDate fecha) {
        return fecha.withDayOfMonth(1);
    }

    public static LocalDate ultimoDiaMes(LocalDate fecha) {
        return fecha.withDayOfMonth(fecha.lengthOfMonth());
    }

    public static long diasEntre(LocalDate desde, LocalDate hasta) {
        return ChronoUnit.DAYS.between(desde, hasta);
    }

    public static boolean esAnteriorAHoy(LocalDate fecha) {
        return fecha.isBefore(LocalDate.now());
    }

    public static int anioActual() {
        return LocalDate.now().getYear();
    }
}
