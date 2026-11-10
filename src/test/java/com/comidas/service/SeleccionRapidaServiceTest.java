package com.comidas.service;

import com.comidas.dto.PlatoOpcion;
import com.comidas.entity.Plato;
import com.comidas.entity.Registro;
import com.comidas.repository.PlatoRepository;
import com.comidas.repository.RegistroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeleccionRapidaServiceTest {

    /** Martes 10/11/2026 a las 13:00 de Argentina (UTC-3). */
    static final Instant AHORA = Instant.parse("2026-11-10T16:00:00Z");

    @Autowired
    PlatoRepository platoRepository;
    @Autowired
    RegistroRepository registroRepository;

    @Test
    void primeroLosMasUsadosEnLaFranjaYDespuesElRestoAlfabetico() {
        Plato milanesa = plato("Milanesa", false);
        Plato ensalada = plato("Ensalada", false);
        Plato avena = plato("avena", false);
        Plato noquis = plato("Ñoquis", false);
        Plato zapallo = plato("Zapallo relleno", true);

        // Dentro de la franja (11:00 a 15:00) y de los últimos 30 días
        comio(ensalada, AHORA.minus(Duration.ofDays(1)).minus(Duration.ofMinutes(30)));
        comio(ensalada, AHORA.minus(Duration.ofDays(3)).plus(Duration.ofMinutes(100)));
        comio(milanesa, AHORA.minus(Duration.ofDays(2)).plus(Duration.ofMinutes(90)));
        // Fuera de la franja: desayunos a las 8:00
        for (int dia = 1; dia <= 3; dia++) {
            comio(avena, AHORA.minus(Duration.ofDays(dia)).minus(Duration.ofHours(5)));
        }
        // Misma hora, pero hace más de 30 días
        for (int i = 0; i < 5; i++) {
            comio(milanesa, AHORA.minus(Duration.ofDays(40 + i)));
        }
        // Muy usado en la franja, pero archivado
        for (int dia = 1; dia <= 4; dia++) {
            comio(zapallo, AHORA.minus(Duration.ofDays(dia)));
        }

        List<PlatoOpcion> opciones = servicio(AHORA).opciones();

        assertThat(opciones).extracting(PlatoOpcion::nombre)
                .containsExactly("Ensalada", "Milanesa", "avena", "Ñoquis");
        assertThat(opciones).extracting(PlatoOpcion::frecuente)
                .containsExactly(true, true, false, false);
        assertThat(opciones).extracting(PlatoOpcion::id).contains(noquis.getId()).doesNotContain(zapallo.getId());
    }

    @Test
    void laFranjaCruzaLaMedianoche() {
        Plato tostado = plato("Tostado", false);
        Plato yogur = plato("Yogur", false);
        // Ahora son las 00:10 de Argentina; anoche a las 23:30 comió un tostado (40 minutos de distancia)
        Instant medianoche = Instant.parse("2026-11-11T03:10:00Z");
        comio(tostado, Instant.parse("2026-11-10T02:30:00Z"));
        // A las 3:00 de la mañana queda fuera de la franja (170 minutos)
        comio(yogur, Instant.parse("2026-11-10T06:00:00Z"));

        List<PlatoOpcion> opciones = servicio(medianoche).opciones();

        assertThat(opciones).extracting(PlatoOpcion::nombre).containsExactly("Tostado", "Yogur");
        assertThat(opciones).extracting(PlatoOpcion::frecuente).containsExactly(true, false);
    }

    @Test
    void distanciaEntreHorasDelDia() {
        assertThat(SeleccionRapidaService.distancia(13 * 60, 11 * 60)).isEqualTo(120);
        assertThat(SeleccionRapidaService.distancia(23 * 60 + 30, 30)).isEqualTo(60);
        assertThat(SeleccionRapidaService.distancia(0, 12 * 60)).isEqualTo(720);
    }

    private SeleccionRapidaService servicio(Instant ahora) {
        return new SeleccionRapidaService(platoRepository, registroRepository, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    private Plato plato(String nombre, boolean archivado) {
        return platoRepository.save(Plato.builder().nombre(nombre).archivado(archivado).build());
    }

    private void comio(Plato plato, Instant cuando) {
        registroRepository.save(Registro.builder().fechaHora(cuando).fotoPath("prueba.jpg").plato(plato).build());
    }
}
