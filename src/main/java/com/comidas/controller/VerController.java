package com.comidas.controller;

import com.comidas.service.LinkCompartidoService;
import com.comidas.service.RegistroService;
import com.comidas.service.SemanaService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de la nutricionista: entra con el link compartido, sin cuenta.
 * <p>
 * Solo lectura: este controller solo tiene GET. Cada pedido (la página y cada foto) valida
 * que el token exista y esté activo; si no, responde 404, así un link revocado deja de
 * mostrar datos y fotos al instante.
 */
@Controller
@RequestMapping("/ver/{token}")
@RequiredArgsConstructor
public class VerController {

    private final LinkCompartidoService linkService;
    private final SemanaService semanaService;
    private final RegistroService registroService;

    @GetMapping
    public String semana(@PathVariable String token,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                         Model model, HttpServletResponse response) {
        linkService.exigirValido(token);
        // Que no aparezca en buscadores ni se filtre el token al salir a otro sitio
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("semana", semanaService.armar(fecha));
        model.addAttribute("token", token);
        return "pages/ver";
    }

    @GetMapping("/fotos/{id}")
    public ResponseEntity<Resource> foto(@PathVariable String token, @PathVariable UUID id) {
        linkService.exigirValido(token);
        return responder(registroService.abrirFoto(id, false));
    }

    @GetMapping("/fotos/{id}/mini")
    public ResponseEntity<Resource> miniatura(@PathVariable String token, @PathVariable UUID id) {
        linkService.exigirValido(token);
        return responder(registroService.abrirFoto(id, true));
    }

    /** Sin caché: una foto guardada en el navegador seguiría viéndose después de revocar el link. */
    private static ResponseEntity<Resource> responder(Resource foto) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(foto);
    }
}
