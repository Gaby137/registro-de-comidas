package com.comidas.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Una semana de lunes a domingo, lista para la vista semanal (la misma para el dueño y la nutricionista).
 *
 * @param titulo    "9 al 15 de noviembre de 2026"
 * @param anterior  lunes de la semana anterior
 * @param siguiente lunes de la semana siguiente
 * @param esActual  true si es la semana de hoy
 */
public record SemanaVista(LocalDate lunes,
                          LocalDate domingo,
                          String titulo,
                          LocalDate anterior,
                          LocalDate siguiente,
                          boolean esActual,
                          List<DiaVista> dias) {

    public int totalRegistros() {
        return dias.stream().mapToInt(d -> d.registros().size()).sum();
    }
}
