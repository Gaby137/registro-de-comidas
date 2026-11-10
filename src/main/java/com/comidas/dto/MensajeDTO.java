package com.comidas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Contrato estándar para devolver mensajes al frontend.
 * <p>
 * Uso para respuestas API:
 * <pre>{@code
 * return ResponseEntity.ok(MensajeDTO.exito("Operación exitosa"));
 * return ResponseEntity.badRequest().body(MensajeDTO.error("Datos inválidos"));
 * }</pre>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeDTO {

    public enum Tipo {
        EXITO, ERROR, ADVERTENCIA, INFO
    }

    private Tipo tipo;
    private String mensaje;
    private List<String> detalles;

    public static MensajeDTO exito(String mensaje) {
        return MensajeDTO.builder().tipo(Tipo.EXITO).mensaje(mensaje).build();
    }

    public static MensajeDTO error(String mensaje) {
        return MensajeDTO.builder().tipo(Tipo.ERROR).mensaje(mensaje).build();
    }

    public static MensajeDTO error(String mensaje, List<String> detalles) {
        return MensajeDTO.builder().tipo(Tipo.ERROR).mensaje(mensaje).detalles(detalles).build();
    }

    public static MensajeDTO advertencia(String mensaje) {
        return MensajeDTO.builder().tipo(Tipo.ADVERTENCIA).mensaje(mensaje).build();
    }

    public static MensajeDTO info(String mensaje) {
        return MensajeDTO.builder().tipo(Tipo.INFO).mensaje(mensaje).build();
    }
}
