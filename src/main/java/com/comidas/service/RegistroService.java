package com.comidas.service;

import com.comidas.dto.RegistroVista;
import com.comidas.entity.Plato;
import com.comidas.entity.Registro;
import com.comidas.exception.ResourceNotFoundException;
import com.comidas.repository.RegistroRepository;
import com.comidas.util.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Registros de comidas: cargar, identificar, editar y borrar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistroService {

    /** Margen por diferencia de reloj entre el celular y el servidor. */
    private static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    /** Una hora de captura más vieja que esto no es un reintento: se usa la del servidor. */
    private static final Duration ANTIGUEDAD_MAXIMA_CAPTURA = Duration.ofDays(7);

    private final RegistroRepository registroRepository;
    private final PlatoService platoService;
    private final FotoStorageService fotoStorage;
    private final Clock clock;

    // === Consultas ===

    public Registro obtener(UUID id) {
        return registroRepository.buscarConPlato(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro no encontrado"));
    }

    public RegistroVista obtenerVista(UUID id) {
        return RegistroVista.de(obtener(id));
    }

    /** Comidas de hoy (hora de Argentina), de la más temprana a la más tardía. */
    public List<RegistroVista> deHoy() {
        LocalDate hoy = ZonaHoraria.hoy(clock);
        return registroRepository
                .buscarEntre(ZonaHoraria.inicioDelDia(hoy), ZonaHoraria.inicioDelDia(hoy.plusDays(1)))
                .stream().map(RegistroVista::de).toList();
    }

    /** Registros sin plato de días anteriores a hoy (los de hoy ya se ven en la pantalla principal). */
    public List<RegistroVista> pendientesAnteriores() {
        LocalDate hoy = ZonaHoraria.hoy(clock);
        return registroRepository.buscarPendientes().stream()
                .map(RegistroVista::de)
                .filter(r -> r.fecha().isBefore(hoy))
                .toList();
    }

    public Resource abrirFoto(UUID id, boolean miniatura) {
        return fotoStorage.abrir(obtener(id).getFotoPath(), miniatura);
    }

    // === Carga ===

    /**
     * Guarda la foto y crea el registro, todavía sin plato.
     *
     * @param capturadoEn momento en que se confirmó la foto en el celular. Así un reintento de subida
     *                    conserva la hora original. Si falta o no es razonable, se usa la del servidor.
     */
    @Transactional
    public Registro crear(MultipartFile foto, Instant capturadoEn) {
        String fotoPath = fotoStorage.guardar(foto);
        try {
            Registro registro = registroRepository.save(Registro.builder()
                    .fechaHora(resolverFechaHora(capturadoEn))
                    .fotoPath(fotoPath)
                    .build());
            log.info("Registro creado: {}", registro.getId());
            return registro;
        } catch (RuntimeException e) {
            fotoStorage.eliminar(fotoPath);
            throw e;
        }
    }

    private Instant resolverFechaHora(Instant capturadoEn) {
        Instant ahora = clock.instant();
        if (capturadoEn == null
                || capturadoEn.isAfter(ahora.plus(TOLERANCIA_FUTURO))
                || capturadoEn.isBefore(ahora.minus(ANTIGUEDAD_MAXIMA_CAPTURA))) {
            return ahora;
        }
        return capturadoEn.isAfter(ahora) ? ahora : capturadoEn;
    }

    @Transactional
    public void asignarPlato(UUID registroId, UUID platoId) {
        Registro registro = obtener(registroId);
        registro.setPlato(platoService.obtener(platoId));
    }

    /** "Plato nuevo" de la selección rápida: lo crea, queda en la lista y se asigna al registro. */
    @Transactional
    public void asignarPlatoNuevo(UUID registroId, String nombre, String descripcion) {
        Registro registro = obtener(registroId);
        registro.setPlato(platoService.crear(nombre, descripcion));
    }

    // === Edición ===

    /**
     * @param platoId        null deja el registro pendiente
     * @param fechaHoraLocal fecha y hora en hora de Argentina
     */
    @Transactional
    public Registro actualizar(UUID id, UUID platoId, LocalDateTime fechaHoraLocal, String nota) {
        Registro registro = obtener(id);
        if (fechaHoraLocal == null) {
            throw new IllegalArgumentException("Ingresá la fecha y la hora de la comida.");
        }
        Instant nuevaFechaHora = ZonaHoraria.instante(fechaHoraLocal);
        if (nuevaFechaHora.isAfter(clock.instant().plus(TOLERANCIA_FUTURO))) {
            throw new IllegalArgumentException("La fecha y hora no pueden ser futuras.");
        }
        String notaLimpia = nota == null ? "" : nota.strip();
        if (notaLimpia.length() > Registro.NOTA_MAX) {
            throw new IllegalArgumentException("La nota no puede tener más de " + Registro.NOTA_MAX + " caracteres.");
        }

        Plato plato = platoId == null ? null : platoService.obtener(platoId);
        registro.setPlato(plato);
        // El formulario trabaja con minutos: si no se cambió, se conservan los segundos originales
        // (mantienen el orden entre dos fotos sacadas en el mismo minuto)
        if (!nuevaFechaHora.equals(registro.getFechaHora().truncatedTo(ChronoUnit.MINUTES))) {
            registro.setFechaHora(nuevaFechaHora);
        }
        registro.setNota(notaLimpia.isEmpty() ? null : notaLimpia);
        return registro;
    }

    /** Borra el registro y su foto (completa y miniatura). */
    @Transactional
    public void borrar(UUID id) {
        Registro registro = obtener(id);
        String fotoPath = registro.getFotoPath();
        registroRepository.delete(registro);
        registroRepository.flush();
        fotoStorage.eliminar(fotoPath);
        log.info("Registro borrado: {}", id);
    }
}
