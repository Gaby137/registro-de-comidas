package com.comidas.service;

import com.comidas.dto.PlatoOpcion;
import com.comidas.entity.Plato;
import com.comidas.repository.PlatoRepository;
import com.comidas.repository.RegistroRepository;
import com.comidas.util.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orden de los platos en la selección rápida.
 * <p>
 * Primero los más usados en esta franja horaria (2 horas antes y después de la hora actual,
 * en los últimos 30 días), de más a menos usado. El resto va debajo, en orden alfabético.
 * Los platos archivados no aparecen.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeleccionRapidaService {

    private static final Duration VENTANA = Duration.ofDays(30);
    private static final int FRANJA_MINUTOS = 120;
    private static final int MINUTOS_POR_DIA = 24 * 60;

    private final PlatoRepository platoRepository;
    private final RegistroRepository registroRepository;
    private final Clock clock;

    public List<PlatoOpcion> opciones() {
        Instant ahora = clock.instant();
        int minutoActual = minutoDelDia(ahora);

        Map<UUID, Long> usosEnFranja = registroRepository.buscarConPlatoDesde(ahora.minus(VENTANA)).stream()
                .filter(r -> !r.getFechaHora().isAfter(ahora))
                .filter(r -> distancia(minutoDelDia(r.getFechaHora()), minutoActual) <= FRANJA_MINUTOS)
                .collect(Collectors.groupingBy(r -> r.getPlato().getId(), Collectors.counting()));

        Comparator<Plato> porNombre = PlatoService.porNombre();
        Function<Plato, Long> usos = p -> usosEnFranja.getOrDefault(p.getId(), 0L);
        List<Plato> activos = platoRepository.findByArchivadoFalse();

        List<PlatoOpcion> opciones = new ArrayList<>(activos.size());
        activos.stream()
                .filter(p -> usos.apply(p) > 0)
                .sorted(Comparator.comparing(usos).reversed().thenComparing(porNombre))
                .forEach(p -> opciones.add(opcion(p, true)));
        activos.stream()
                .filter(p -> usos.apply(p) == 0)
                .sorted(porNombre)
                .forEach(p -> opciones.add(opcion(p, false)));
        return opciones;
    }

    private static PlatoOpcion opcion(Plato plato, boolean frecuente) {
        return new PlatoOpcion(plato.getId(), plato.getNombre(), plato.getDescripcion(), frecuente);
    }

    private static int minutoDelDia(Instant instante) {
        return ZonaHoraria.local(instante).get(ChronoField.MINUTE_OF_DAY);
    }

    /** Distancia entre dos horas del día cruzando la medianoche: 23:30 y 00:30 están a 60 minutos. */
    static int distancia(int minutoA, int minutoB) {
        int directa = Math.abs(minutoA - minutoB);
        return Math.min(directa, MINUTOS_POR_DIA - directa);
    }
}
