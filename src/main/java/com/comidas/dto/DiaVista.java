package com.comidas.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Una columna de la vista semanal. Un día sin registros se muestra vacío, no se oculta.
 *
 * @param nombre     "lunes"
 * @param fechaCorta "9/11"
 */
public record DiaVista(LocalDate fecha, String nombre, String fechaCorta, boolean hoy, List<RegistroVista> registros) {
}
