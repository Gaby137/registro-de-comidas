package com.comidas.dto;

import java.util.UUID;

/**
 * Un botón de la selección rápida.
 *
 * @param frecuente true si es de los más usados en esta franja horaria (van primero)
 */
public record PlatoOpcion(UUID id, String nombre, String descripcion, boolean frecuente) {
}
