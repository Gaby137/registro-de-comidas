package com.comidas.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Duration;

/**
 * Manifest de la PWA. Se sirve desde acá (y no como estático) para que salga con su tipo MIME
 * {@code application/manifest+json}: como archivo estático Spring lo manda como octet-stream.
 */
@Controller
public class PwaController {

    private static final MediaType MANIFEST = MediaType.parseMediaType("application/manifest+json");

    @GetMapping("/manifest.webmanifest")
    public ResponseEntity<Resource> manifest() {
        return ResponseEntity.ok()
                .contentType(MANIFEST)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body(new ClassPathResource("pwa/manifest.webmanifest"));
    }
}
