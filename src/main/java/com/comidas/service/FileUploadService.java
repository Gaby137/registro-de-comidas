package com.comidas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio genérico de subida de archivos con validaciones de seguridad.
 * <p>
 * Valida: tamaño máximo, extensión, MIME type y que los magic bytes coincidan con la extensión.
 * Guarda con nombre UUID (nunca el nombre original) y devuelve la URL pública "/uploads/...".
 * <p>
 * Uso en un controller MVC:
 * <pre>{@code
 * @PostMapping("/productos/{id}/imagen")
 * public String subir(@PathVariable Long id, @RequestParam MultipartFile archivo) throws IOException {
 *     String url = fileUploadService.guardarArchivo(archivo);
 *     productoService.actualizarImagen(id, url);
 *     return "redirect:/productos/" + id;
 * }
 * }</pre>
 */
@Service
@Slf4j
public class FileUploadService {

    private static final String URL_PREFIX = "/uploads/";
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    /** Extensión permitida → MIME type esperado. Para aceptar otro formato, agregalo acá y en firmaValida(). */
    private static final Map<String, String> FORMATOS_PERMITIDOS = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp",
            ".gif", "image/gif",
            ".pdf", "application/pdf"
    );

    private final Path uploadPath;

    public FileUploadService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Guarda un archivo validando extensión, MIME type, tamaño y magic bytes.
     *
     * @param archivo El archivo a guardar
     * @return La URL relativa del archivo guardado (ej: "/uploads/abc123.jpg")
     * @throws IllegalArgumentException si el archivo no es válido (mensaje apto para mostrar al usuario)
     */
    public String guardarArchivo(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("No se puede subir un archivo vacío.");
        }

        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo es demasiado pesado. El límite máximo es 5MB.");
        }

        String extension = extraerExtension(archivo.getOriginalFilename());
        String mimeEsperado = FORMATOS_PERMITIDOS.get(extension);
        if (mimeEsperado == null) {
            throw new IllegalArgumentException("Formato de archivo no permitido. Se aceptan: "
                    + String.join(", ", FORMATOS_PERMITIDOS.keySet().stream().sorted().toList()));
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !mimeEsperado.equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("El tipo de contenido del archivo no es válido.");
        }

        validarMagicBytes(archivo, mimeEsperado);

        Files.createDirectories(uploadPath);

        String nuevoNombre = UUID.randomUUID() + extension;
        try (InputStream is = archivo.getInputStream()) {
            Files.copy(is, uploadPath.resolve(nuevoNombre), StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Archivo guardado: {}", nuevoNombre);
        return URL_PREFIX + nuevoNombre;
    }

    /**
     * Elimina un archivo por su URL relativa ("/uploads/abc.jpg").
     * Ignora cualquier ruta que intente salir del directorio de uploads (ej: "/uploads/../x").
     */
    public void eliminarArchivo(String urlRelativa) {
        if (urlRelativa == null || !urlRelativa.startsWith(URL_PREFIX)) return;

        Path rutaArchivo = uploadPath.resolve(urlRelativa.substring(URL_PREFIX.length())).normalize();
        if (!rutaArchivo.getParent().equals(uploadPath)) {
            log.warn("Intento de eliminar un archivo fuera del directorio de uploads: {}", urlRelativa);
            return;
        }

        try {
            Files.deleteIfExists(rutaArchivo);
            log.info("Archivo eliminado: {}", rutaArchivo.getFileName());
        } catch (IOException e) {
            log.error("No se pudo eliminar el archivo: {}", rutaArchivo.getFileName(), e);
        }
    }

    private String extraerExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i).toLowerCase(Locale.ROOT) : "";
    }

    /**
     * Verifica que los magic bytes (firma) del contenido coincidan con el formato declarado.
     * Previene archivos maliciosos con extensión cambiada.
     */
    private void validarMagicBytes(MultipartFile archivo, String mime) throws IOException {
        byte[] header;
        try (InputStream is = archivo.getInputStream()) {
            header = is.readNBytes(12);
        }
        if (header.length < 4 || !firmaValida(header, mime)) {
            throw new IllegalArgumentException("El contenido del archivo no coincide con su formato.");
        }
    }

    private boolean firmaValida(byte[] h, String mime) {
        return switch (mime) {
            case "image/jpeg" -> empiezaCon(h, 0, 0xFF, 0xD8, 0xFF);
            case "image/png" -> empiezaCon(h, 0, 0x89, 'P', 'N', 'G');
            case "image/gif" -> empiezaCon(h, 0, 'G', 'I', 'F', '8');
            case "application/pdf" -> empiezaCon(h, 0, '%', 'P', 'D', 'F');
            // WebP: "RIFF" + tamaño (4 bytes) + "WEBP"
            case "image/webp" -> empiezaCon(h, 0, 'R', 'I', 'F', 'F') && empiezaCon(h, 8, 'W', 'E', 'B', 'P');
            default -> false;
        };
    }

    private boolean empiezaCon(byte[] data, int offset, int... firma) {
        if (data.length < offset + firma.length) return false;
        for (int i = 0; i < firma.length; i++) {
            if ((data[offset + i] & 0xFF) != firma[i]) return false;
        }
        return true;
    }
}
