package com.comidas.service;

import com.comidas.dto.DiaVista;
import com.comidas.dto.RegistroVista;
import com.comidas.dto.SemanaVista;
import com.comidas.repository.RegistroRepository;
import com.comidas.util.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Arma la vista semanal: siete días de lunes a domingo, en hora de Argentina,
 * con las comidas de cada día ordenadas de la más temprana a la más tardía.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemanaService {

    private final RegistroRepository registroRepository;
    private final Clock clock;

    /**
     * @param fecha cualquier día de la semana a mostrar; null = semana actual
     */
    public SemanaVista armar(LocalDate fecha) {
        LocalDate hoy = ZonaHoraria.hoy(clock);
        LocalDate lunes = lunesDe(fecha != null ? fecha : hoy);
        LocalDate domingo = lunes.plusDays(6);

        Map<LocalDate, List<RegistroVista>> porDia = registroRepository
                .buscarEntre(ZonaHoraria.inicioDelDia(lunes), ZonaHoraria.inicioDelDia(lunes.plusDays(7)))
                .stream()
                .map(RegistroVista::de)
                .collect(Collectors.groupingBy(RegistroVista::fecha));

        List<DiaVista> dias = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunes.plusDays(i);
            dias.add(new DiaVista(
                    dia,
                    dia.getDayOfWeek().getDisplayName(TextStyle.FULL, ZonaHoraria.ES),
                    dia.getDayOfMonth() + "/" + dia.getMonthValue(),
                    dia.equals(hoy),
                    porDia.getOrDefault(dia, List.of())));
        }

        return new SemanaVista(lunes, domingo, titulo(lunes, domingo),
                lunes.minusWeeks(1), lunes.plusWeeks(1), lunes.equals(lunesDe(hoy)), dias);
    }

    private static LocalDate lunesDe(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** "9 al 15 de noviembre de 2026" o, si cruza de mes, "30 de noviembre al 6 de diciembre de 2026". */
    private static String titulo(LocalDate lunes, LocalDate domingo) {
        String mesDomingo = domingo.getMonth().getDisplayName(TextStyle.FULL, ZonaHoraria.ES);
        if (lunes.getMonth() == domingo.getMonth()) {
            return "%d al %d de %s de %d".formatted(
                    lunes.getDayOfMonth(), domingo.getDayOfMonth(), mesDomingo, domingo.getYear());
        }
        String mesLunes = lunes.getMonth().getDisplayName(TextStyle.FULL, ZonaHoraria.ES);
        return "%d de %s al %d de %s de %d".formatted(
                lunes.getDayOfMonth(), mesLunes, domingo.getDayOfMonth(), mesDomingo, domingo.getYear());
    }
}
