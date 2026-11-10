package com.comidas;

import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Fotos de prueba generadas en memoria.
 */
public final class Fotos {

    private Fotos() {}

    public static byte[] jpeg(int ancho, int alto) {
        return codificar(ancho, alto, "jpg");
    }

    public static byte[] png(int ancho, int alto) {
        return codificar(ancho, alto, "png");
    }

    /** Parte multipart "foto" tal como la manda carga.js. */
    public static MockMultipartFile parteJpeg(int ancho, int alto) {
        return new MockMultipartFile("foto", "foto.jpg", "image/jpeg", jpeg(ancho, alto));
    }

    private static byte[] codificar(int ancho, int alto, String formato) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(new Color(200, 120, 40));
        g.fillRect(0, 0, ancho, alto);
        g.setColor(Color.WHITE);
        g.fillOval(ancho / 4, alto / 4, ancho / 2, alto / 2);
        g.dispose();
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, formato, salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
