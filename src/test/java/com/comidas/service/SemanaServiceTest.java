package com.comidas.service;

import com.comidas.dto.DiaVista;
import com.comidas.dto.RegistroVista;
import com.comidas.dto.SemanaVista;
import com.comidas.entity.Plato;
import com.comidas.entity.Registro;
import com.comidas.repository.PlatoRepository;
import com.comidas.repository.RegistroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SemanaServiceTest {

    /** Miércoles 11/11/2026 a las 12:00 de Argentina. */
    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-11T15:00:00Z"), ZoneOffset.UTC);

    @Autowired
    RegistroRepository registroRepository;
    @Autowired
    PlatoRepository platoRepository;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void sieteDiasDeLunesADomingoEnHoraDeArgentina() {
        Plato milanesa = platoRepository.save(
                Plato.builder().nombre("Milanesa").descripcion("Con ensalada").build());

        // Domingo 8/11 a las 23:59 de Argentina: es de la semana anterior
        registro("2026-11-09T02:59:00Z", null, null);
        // Lunes 9/11 a las 00:00 de Argentina: primer momento de la semana
        registro("2026-11-09T03:00:00Z", null, null);
        // Miércoles 11/11: cena cargada antes que el almuerzo; tienen que salir por hora
        registro("2026-11-12T00:15:00Z", milanesa, "comí la mitad");
        registro("2026-11-11T16:05:00Z", milanesa, null);
        // Domingo 15/11 a las 23:30 de Argentina: en UTC ya es lunes 16
        registro("2026-11-16T02:30:00Z", milanesa, null);
        // Lunes 16/11 a las 00:00 de Argentina: semana siguiente
        registro("2026-11-16T03:00:00Z", null, null);

        SemanaVista semana = new SemanaService(registroRepository, RELOJ).armar(null);

        assertThat(semana.lunes()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(semana.domingo()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(semana.titulo()).isEqualTo("9 al 15 de noviembre de 2026");
        assertThat(semana.esActual()).isTrue();
        assertThat(semana.anterior()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(semana.siguiente()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(semana.totalRegistros()).isEqualTo(4);

        // Un día sin registros se muestra vacío, no se oculta
        assertThat(semana.dias()).hasSize(7);
        assertThat(semana.dias()).extracting(DiaVista::nombre).containsExactly(
                "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo");
        assertThat(semana.dias()).extracting(d -> d.registros().size()).containsExactly(1, 0, 2, 0, 0, 0, 1);
        assertThat(semana.dias()).extracting(DiaVista::hoy)
                .containsExactly(false, false, true, false, false, false, false);

        DiaVista lunes = semana.dias().get(0);
        assertThat(lunes.fechaCorta()).isEqualTo("9/11");
        assertThat(lunes.registros().get(0).hora()).isEqualTo("00:00");
        assertThat(lunes.registros().get(0).pendiente()).isTrue();

        DiaVista miercoles = semana.dias().get(2);
        assertThat(miercoles.registros()).extracting(RegistroVista::hora).containsExactly("13:05", "21:15");
        RegistroVista cena = miercoles.registros().get(1);
        assertThat(cena.platoNombre()).isEqualTo("Milanesa");
        assertThat(cena.platoDescripcion()).isEqualTo("Con ensalada");
        assertThat(cena.nota()).isEqualTo("comí la mitad");
        assertThat(cena.fechaLarga()).isEqualTo("miércoles 11 de noviembre de 2026");

        assertThat(semana.dias().get(6).registros().get(0).hora()).isEqualTo("23:30");
    }

    @Test
    void cualquierFechaLlevaASuSemana() {
        SemanaService servicio = new SemanaService(registroRepository, RELOJ);

        // Miércoles 2/12/2026: semana que cruza de mes
        SemanaVista semana = servicio.armar(LocalDate.of(2026, 12, 2));

        assertThat(semana.lunes()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(semana.titulo()).isEqualTo("30 de noviembre al 6 de diciembre de 2026");
        assertThat(semana.esActual()).isFalse();
        assertThat(semana.dias()).allMatch(d -> d.registros().isEmpty() && !d.hoy());

        // Un domingo pertenece a la semana que empezó el lunes anterior
        assertThat(servicio.armar(LocalDate.of(2026, 11, 15)).lunes()).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    void lasFechasSeGuardanEnUtcYLosIdsComoTexto() {
        Registro registro = registro("2026-11-10T16:05:30Z", null, null);
        registroRepository.flush();

        String guardada = jdbc.queryForObject(
                "select cast(fecha_hora as varchar) from registros where id = ?", String.class,
                registro.getId().toString());

        // 16:05 UTC son las 13:05 de Argentina: en la base tiene que quedar la hora UTC
        assertThat(guardada).startsWith("2026-11-10 16:05:30");
    }

    private Registro registro(String instante, Plato plato, String nota) {
        return registroRepository.save(Registro.builder()
                .fechaHora(Instant.parse(instante)).fotoPath("prueba.jpg").plato(plato).nota(nota).build());
    }
}
