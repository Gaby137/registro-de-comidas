package com.comidas.service;

import com.comidas.entity.Plato;
import com.comidas.exception.ResourceNotFoundException;
import com.comidas.repository.PlatoRepository;
import com.comidas.util.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Platos precargados. Se crean, se editan y se archivan; nunca se borran,
 * porque los registros viejos tienen que seguir mostrándolos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatoService {

    private final PlatoRepository platoRepository;

    /** Orden alfabético en español: ignora mayúsculas y acentos ("Ñoquis" después de "Milanesa"). */
    public static Comparator<Plato> porNombre() {
        Collator collator = Collator.getInstance(ZonaHoraria.ES);
        collator.setStrength(Collator.PRIMARY);
        return Comparator.comparing(Plato::getNombre, collator);
    }

    /** Todos los platos: primero los activos, después los archivados; cada grupo en orden alfabético. */
    public List<Plato> listarTodos() {
        return platoRepository.findAll().stream()
                .sorted(Comparator.comparing(Plato::isArchivado).thenComparing(porNombre()))
                .toList();
    }

    public List<Plato> listarActivos() {
        return platoRepository.findByArchivadoFalse().stream().sorted(porNombre()).toList();
    }

    public Plato obtener(UUID id) {
        return platoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plato no encontrado"));
    }

    @Transactional
    public Plato crear(String nombre, String descripcion) {
        String nombreLimpio = validarNombre(nombre);
        if (platoRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException("Ya existe un plato con ese nombre (puede estar archivado).");
        }
        Plato plato = platoRepository.save(Plato.builder()
                .nombre(nombreLimpio)
                .descripcion(validarDescripcion(descripcion))
                .build());
        log.info("Plato creado: {}", plato.getNombre());
        return plato;
    }

    /** Editar la descripción cambia lo que se ve en todos los registros de ese plato. */
    @Transactional
    public Plato actualizar(UUID id, String nombre, String descripcion) {
        Plato plato = obtener(id);
        String nombreLimpio = validarNombre(nombre);
        if (platoRepository.existsByNombreIgnoreCaseAndIdNot(nombreLimpio, id)) {
            throw new IllegalArgumentException("Ya existe otro plato con ese nombre (puede estar archivado).");
        }
        plato.setNombre(nombreLimpio);
        plato.setDescripcion(validarDescripcion(descripcion));
        return plato;
    }

    /** Un plato archivado no aparece en la selección rápida, pero sus registros lo siguen mostrando. */
    @Transactional
    public Plato cambiarArchivado(UUID id, boolean archivado) {
        Plato plato = obtener(id);
        plato.setArchivado(archivado);
        log.info("Plato {} {}", plato.getNombre(), archivado ? "archivado" : "restaurado");
        return plato;
    }

    private String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.strip().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El plato necesita un nombre.");
        }
        if (limpio.length() > Plato.NOMBRE_MAX) {
            throw new IllegalArgumentException("El nombre no puede tener más de " + Plato.NOMBRE_MAX + " caracteres.");
        }
        return limpio;
    }

    private String validarDescripcion(String descripcion) {
        String limpia = descripcion == null ? "" : descripcion.strip();
        if (limpia.length() > Plato.DESCRIPCION_MAX) {
            throw new IllegalArgumentException(
                    "La descripción no puede tener más de " + Plato.DESCRIPCION_MAX + " caracteres.");
        }
        return limpia.isEmpty() ? null : limpia;
    }
}
