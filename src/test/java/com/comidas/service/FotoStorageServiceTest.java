package com.comidas.service;

import com.comidas.Fotos;
import com.comidas.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FotoStorageServiceTest {

    @TempDir
    Path directorio;

    FotoStorageService storage;

    @BeforeEach
    void crear() {
        storage = new FotoStorageService(directorio.toString());
    }

    @Test
    void guardaLaFotoTalCualYGeneraLaMiniatura() throws IOException {
        byte[] original = Fotos.jpeg(1600, 1200);

        String nombre = storage.guardar(new MockMultipartFile("foto", "foto.jpg", "image/jpeg", original));

        assertThat(nombre).matches("[0-9a-f-]{36}\\.jpg");
        assertThat(Files.readAllBytes(directorio.resolve(nombre))).isEqualTo(original);

        Resource miniatura = storage.abrir(nombre, true);
        try (InputStream in = miniatura.getInputStream()) {
            BufferedImage imagen = ImageIO.read(in);
            assertThat(imagen.getWidth()).isEqualTo(400);
            assertThat(imagen.getHeight()).isEqualTo(300);
        }
        assertThat(miniatura.contentLength()).isLessThan(original.length);
    }

    @Test
    void unaFotoChicaNoSeAgranda() throws IOException {
        String nombre = storage.guardar(Fotos.parteJpeg(200, 100));

        try (InputStream in = storage.abrir(nombre, true).getInputStream()) {
            BufferedImage imagen = ImageIO.read(in);
            assertThat(imagen.getWidth()).isEqualTo(200);
            assertThat(imagen.getHeight()).isEqualTo(100);
        }
    }

    @Test
    void rechazaLoQueNoEsJpeg() {
        // Otro formato declarado
        assertThatThrownBy(() -> storage.guardar(
                new MockMultipartFile("foto", "foto.png", "image/png", Fotos.png(50, 50))))
                .isInstanceOf(IllegalArgumentException.class);
        // PNG disfrazado de JPEG: lo delatan los magic bytes
        assertThatThrownBy(() -> storage.guardar(
                new MockMultipartFile("foto", "foto.jpg", "image/jpeg", Fotos.png(50, 50))))
                .isInstanceOf(IllegalArgumentException.class);
        // Empieza como un JPEG pero no se puede decodificar
        byte[] basura = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5, 6, 7};
        assertThatThrownBy(() -> storage.guardar(
                new MockMultipartFile("foto", "foto.jpg", "image/jpeg", basura)))
                .isInstanceOf(IllegalArgumentException.class);
        // Vacío
        assertThatThrownBy(() -> storage.guardar(
                new MockMultipartFile("foto", "foto.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(directorio).isEmptyDirectory();
    }

    @Test
    void eliminarBorraLaFotoYSuMiniatura() {
        String nombre = storage.guardar(Fotos.parteJpeg(800, 600));
        assertThat(directorio.toFile().list()).hasSize(2);

        storage.eliminar(nombre);

        assertThat(directorio).isEmptyDirectory();
        assertThatThrownBy(() -> storage.abrir(nombre, false)).isInstanceOf(ResourceNotFoundException.class);
        // Borrar algo que ya no está no falla
        storage.eliminar(nombre);
    }

    @Test
    void noSaleDelDirectorioDeFotos() throws IOException {
        Path afuera = Files.createTempFile(directorio.getParent(), "secreto", ".jpg");
        try {
            String traversal = "../" + afuera.getFileName();
            assertThatThrownBy(() -> storage.abrir(traversal, false)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> storage.eliminar(traversal)).isInstanceOf(ResourceNotFoundException.class);
            assertThat(afuera).exists();
        } finally {
            Files.deleteIfExists(afuera);
        }
    }
}
