package com.comidas.controller;

import com.comidas.dto.MensajeDTO;
import com.comidas.entity.Registro;
import com.comidas.exception.ResourceNotFoundException;
import com.comidas.service.RegistroService;
import com.comidas.service.SeleccionRapidaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/**
 * Flujo "cargar una comida" (lo consume carga.js con fetch): subir la foto, elegir el plato.
 * Responde JSON; los errores de negocio vuelven como {@link MensajeDTO}.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CargaController {

    private final RegistroService registroService;
    private final SeleccionRapidaService seleccionRapidaService;

    /** Sube la foto y crea el registro pendiente. El plato se asigna en un segundo pedido. */
    @PostMapping("/registros")
    @ResponseBody
    public ResponseEntity<Map<String, String>> subirFoto(@RequestParam("foto") MultipartFile foto,
                                                         @RequestParam(required = false) String capturadoEn) {
        Registro registro = registroService.crear(foto, parsearInstante(capturadoEn));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", registro.getId().toString()));
    }

    @PostMapping("/registros/{id}/plato")
    @ResponseBody
    public MensajeDTO asignarPlato(@PathVariable UUID id, @RequestParam UUID platoId) {
        registroService.asignarPlato(id, platoId);
        return MensajeDTO.exito("Comida guardada.");
    }

    @PostMapping("/registros/{id}/plato-nuevo")
    @ResponseBody
    public MensajeDTO asignarPlatoNuevo(@PathVariable UUID id,
                                        @RequestParam String nombre,
                                        @RequestParam(required = false) String descripcion) {
        registroService.asignarPlatoNuevo(id, nombre, descripcion);
        return MensajeDTO.exito("Comida guardada.");
    }

    /** Fragmento HTML con los platos como botones, ordenados para la hora actual. */
    @GetMapping("/seleccion-rapida")
    public String seleccionRapida(Model model) {
        model.addAttribute("opciones", seleccionRapidaService.opciones());
        return "fragments/seleccion-rapida :: lista";
    }

    /**
     * Token CSRF vigente. La app instalada puede quedar abierta más que la sesión:
     * si un POST vuelve 403, carga.js pide el token nuevo acá y reintenta.
     */
    @GetMapping("/csrf")
    @ResponseBody
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("header", token.getHeaderName(), "token", token.getToken());
    }

    private static Instant parsearInstante(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // === Errores en JSON (este controller no devuelve vistas de error) ===

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MensajeDTO> reglaDeNegocio(IllegalArgumentException e) {
        log.warn("Carga rechazada: {}", e.getMessage());
        return ResponseEntity.badRequest().body(MensajeDTO.error(e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MensajeDTO> noEncontrado(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(MensajeDTO.error(e.getMessage()));
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<MensajeDTO> fotoInvalida(Exception e) {
        log.warn("Error de subida: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(MensajeDTO.error("La foto es demasiado pesada (máximo 5 MB)."));
    }
}
