package com.comidas.controller;

import com.comidas.service.RegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;
import java.util.UUID;

/**
 * Fotos para el dueño. Estas rutas exigen sesión (ver SecurityConfig);
 * la nutricionista las pide por {@code /ver/<token>/fotos/...} (VerController).
 */
@Controller
@RequiredArgsConstructor
public class FotoController {

    /** La foto de un registro no cambia nunca: el navegador del dueño puede guardarla. */
    private static final CacheControl CACHE_DUENO = CacheControl.maxAge(Duration.ofDays(7)).cachePrivate();

    private final RegistroService registroService;

    @GetMapping("/fotos/{id}")
    public ResponseEntity<Resource> foto(@PathVariable UUID id) {
        return responder(registroService.abrirFoto(id, false));
    }

    @GetMapping("/fotos/{id}/mini")
    public ResponseEntity<Resource> miniatura(@PathVariable UUID id) {
        return responder(registroService.abrirFoto(id, true));
    }

    private static ResponseEntity<Resource> responder(Resource foto) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CACHE_DUENO)
                .header("X-Content-Type-Options", "nosniff")
                .body(foto);
    }
}
