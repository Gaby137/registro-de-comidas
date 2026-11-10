package com.comidas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    private static final byte[] WAV = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};

    @TempDir
    Path tempDir;
    Path uploads;
    FileUploadService service;

    @BeforeEach
    void setUp() {
        uploads = tempDir.resolve("uploads");
        service = new FileUploadService(uploads.toString());
    }

    @Test
    void guardaArchivoValidoConNombreUuid() throws Exception {
        String url = service.guardarArchivo(new MockMultipartFile("f", "foto.PNG", "image/png", PNG));

        assertThat(url).startsWith("/uploads/").endsWith(".png").doesNotContain("foto");
        assertThat(Files.exists(uploads.resolve(url.substring("/uploads/".length())))).isTrue();
    }

    @Test
    void aceptaWebpReal() throws Exception {
        assertThat(service.guardarArchivo(new MockMultipartFile("f", "a.webp", "image/webp", WEBP))).endsWith(".webp");
    }

    @Test
    void rechazaRiffQueNoEsWebp() {
        assertThatThrownBy(() -> service.guardarArchivo(new MockMultipartFile("f", "a.webp", "image/webp", WAV)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaContenidoQueNoCoincideConLaExtension() {
        // PDF renombrado a .png con MIME de imagen
        assertThatThrownBy(() -> service.guardarArchivo(new MockMultipartFile("f", "x.png", "image/png", PDF)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaExtensionNoPermitida() {
        assertThatThrownBy(() -> service.guardarArchivo(new MockMultipartFile("f", "x.svg", "image/svg+xml", PNG)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no permitido");
    }

    @Test
    void rechazaArchivoSinNombre() {
        assertThatThrownBy(() -> service.guardarArchivo(new MockMultipartFile("f", null, "image/png", PNG)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eliminarNoSaleDelDirectorioDeUploads() throws Exception {
        Path afuera = Files.writeString(tempDir.resolve("importante.txt"), "no borrar");
        Files.createDirectories(uploads);

        service.eliminarArchivo("/uploads/../importante.txt");

        assertThat(Files.exists(afuera)).isTrue();
    }

    @Test
    void eliminaArchivoSubido() throws Exception {
        String url = service.guardarArchivo(new MockMultipartFile("f", "a.png", "image/png", PNG));

        service.eliminarArchivo(url);

        assertThat(Files.list(uploads)).isEmpty();
    }
}
