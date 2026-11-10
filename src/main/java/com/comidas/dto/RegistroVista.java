package com.comidas.dto;

import com.comidas.entity.Plato;
import com.comidas.entity.Registro;
import com.comidas.util.ZonaHoraria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Una comida lista para mostrar: fechas ya en hora de Argentina y formato de 24 horas.
 * Se arma dentro del service (open-in-view está apagado).
 *
 * @param fechaLarga  "martes 10 de noviembre de 2026"
 * @param fechaCorta  "mar 10/11"
 * @param pendiente   true si todavía no tiene plato
 */
public record RegistroVista(UUID id,
                            LocalDate fecha,
                            String hora,
                            String fechaLarga,
                            String fechaCorta,
                            String platoNombre,
                            String platoDescripcion,
                            String nota,
                            boolean pendiente) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ZonaHoraria.ES);
    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("EEE d/M", ZonaHoraria.ES);

    public static RegistroVista de(Registro registro) {
        LocalDateTime local = ZonaHoraria.local(registro.getFechaHora());
        Plato plato = registro.getPlato();
        return new RegistroVista(
                registro.getId(),
                local.toLocalDate(),
                local.format(HORA),
                local.format(LARGA),
                local.format(CORTA),
                plato != null ? plato.getNombre() : null,
                plato != null ? plato.getDescripcion() : null,
                registro.getNota(),
                plato == null);
    }
}
