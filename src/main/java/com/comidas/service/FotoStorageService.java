package com.comidas.service;

import com.comidas.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Almacenamiento PRIVADO de las fotos de comidas.
 * <p>
 * El directorio no se expone como recurso estático: las fotos solo salen por un controller
 * que antes validó la sesión del dueño o un token de link activo.
 * <p>
 * Cada foto son dos archivos: {@code <uuid>.jpg} (la que comprimió el celular) y
 * {@code <uuid>_mini.jpg} (miniatura para la vista semanal, generada acá).
 */
@Service
@Slf4j
public class FotoStorageService {

    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final int MINIATURA_LADO_MAYOR = 400;
    private static final float MINIATURA_CALIDAD = 0.8f;
    private static final String SUFIJO_MINIATURA = "_mini.jpg";
    /** Los nombres los genera este servicio: cualquier otra cosa (rutas, "..") se rechaza. */
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[0-9a-f-]{36}\\.jpg");

    private final Path raiz;

    public FotoStorageService(@Value("${app.fotos.dir}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    /**
     * Valida y guarda una foto, y genera su miniatura.
     *
     * @return el nombre del archivo guardado (lo que va en {@code registros.foto_path})
     * @throws IllegalArgumentException si no es un JPEG válido (mensaje apto para mostrar)
     */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("No llegó ninguna foto.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("La foto es demasiado pesada (máximo 5 MB).");
        }
        if (!"image/jpeg".equalsIgnoreCase(archivo.getContentType())) {
            throw new IllegalArgumentException("La foto tiene que ser un JPEG.");
        }

        try {
            byte[] bytes = archivo.getBytes();
            if (!esJpeg(bytes)) {
                throw new IllegalArgumentException("El archivo no es una foto JPEG válida.");
            }
            BufferedImage imagen = decodificar(bytes);

            Files.createDirectories(raiz);
            String nombre = UUID.randomUUID() + ".jpg";
            Path destino = raiz.resolve(nombre);
            Files.write(destino, bytes);
            try {
                escribirMiniatura(imagen, raiz.resolve(nombreMiniatura(nombre)));
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(destino);
                throw e;
            }
            log.info("Foto guardada: {}", nombre);
            return nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto", e);
        }
    }

    /** Abre la foto completa o su miniatura. */
    public Resource abrir(String fotoPath, boolean miniatura) {
        Path archivo = resolver(miniatura ? nombreMiniatura(fotoPath) : fotoPath);
        if (!Files.isRegularFile(archivo)) {
            throw new ResourceNotFoundException("Foto no encontrada");
        }
        return new FileSystemResource(archivo);
    }

    /** Borra la foto y su miniatura. Si algún archivo ya no está, no falla. */
    public void eliminar(String fotoPath) {
        for (String nombre : new String[]{fotoPath, nombreMiniatura(fotoPath)}) {
            try {
                Files.deleteIfExists(resolver(nombre));
            } catch (IOException e) {
                log.error("No se pudo eliminar la foto {}", nombre, e);
            }
        }
        log.info("Foto eliminada: {}", fotoPath);
    }

    private Path resolver(String nombre) {
        String base = nombre == null ? "" : nombre.replace(SUFIJO_MINIATURA, ".jpg");
        if (!NOMBRE_VALIDO.matcher(base).matches()) {
            throw new ResourceNotFoundException("Foto no encontrada");
        }
        return raiz.resolve(nombre);
    }

    private static String nombreMiniatura(String fotoPath) {
        return fotoPath == null ? null : fotoPath.replace(".jpg", SUFIJO_MINIATURA);
    }

    /** Magic bytes de JPEG: evita guardar otra cosa con el Content-Type cambiado. */
    private static boolean esJpeg(byte[] bytes) {
        return bytes.length > 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
    }

    private static BufferedImage decodificar(byte[] bytes) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(bytes));
            if (imagen != null) {
                return imagen;
            }
        } catch (IOException e) {
            log.warn("JPEG ilegible: {}", e.getMessage());
        }
        throw new IllegalArgumentException("No se pudo leer la foto. Probá sacarla de nuevo.");
    }

    private static void escribirMiniatura(BufferedImage original, Path destino) throws IOException {
        BufferedImage miniatura = reducir(original);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ImageOutputStream salida = ImageIO.createImageOutputStream(destino.toFile())) {
            ImageWriteParam parametros = writer.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(MINIATURA_CALIDAD);
            writer.setOutput(salida);
            writer.write(null, new IIOImage(miniatura, null, null), parametros);
        } finally {
            writer.dispose();
        }
    }

    /** Reduce a la mitad en pasos sucesivos (mejor calidad que un solo salto) hasta el tamaño de miniatura. */
    private static BufferedImage reducir(BufferedImage original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        double escala = Math.min(1.0, (double) MINIATURA_LADO_MAYOR / Math.max(ancho, alto));
        int anchoFinal = Math.max(1, (int) Math.round(ancho * escala));
        int altoFinal = Math.max(1, (int) Math.round(alto * escala));

        BufferedImage actual = original;
        while (ancho / 2 >= anchoFinal && alto / 2 >= altoFinal) {
            ancho /= 2;
            alto /= 2;
            actual = escalar(actual, ancho, alto);
        }
        // Siempre termina en una imagen RGB propia, aunque no haya que achicar
        return escalar(actual, anchoFinal, altoFinal);
    }

    private static BufferedImage escalar(BufferedImage origen, int ancho, int alto) {
        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(origen, 0, 0, ancho, alto, null);
        } finally {
            g.dispose();
        }
        return destino;
    }
}
