package com.comidas.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Formulario de edición de un registro.
 * {@code fechaHora} viene de un {@code <input type="datetime-local">} y se interpreta en hora de Argentina.
 */
@Data
public class RegistroForm {

    /** Vacío = dejar el registro pendiente. */
    private UUID platoId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaHora;

    private String nota;
}
