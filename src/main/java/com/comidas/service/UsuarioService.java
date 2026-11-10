package com.comidas.service;

import com.comidas.dto.request.RegistroRequest;
import com.comidas.entity.Usuario;
import com.comidas.enums.Rol;
import com.comidas.exception.ResourceNotFoundException;
import com.comidas.repository.UsuarioRepository;
import com.comidas.specification.UsuarioSpecification;
import com.comidas.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Lógica de negocio de usuarios. La usan tanto los controllers MVC como los de la API:
 * los controllers solo traducen HTTP ↔ servicio, nunca tocan el repositorio directo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Emails siempre en minúsculas y sin espacios, para que "Juan@Mail.com" y "juan@mail.com" sean el mismo. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public Usuario registrar(RegistroRequest request) {
        String email = normalizarEmail(request.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .email(email)
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .telefono(StringUtils.isNullOrBlank(request.getTelefono()) ? null : request.getTelefono().trim())
                .rol(Rol.USER)
                .build();

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Nuevo usuario registrado: {}", saved.getEmail());
        return saved;
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(normalizarEmail(email));
    }

    public Usuario obtenerPorEmail(String email) {
        return buscarPorEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    /**
     * Búsqueda paginada con filtros opcionales (null = no filtrar).
     * Ejemplo del patrón Specification: ver UsuarioSpecification.
     */
    public Page<Usuario> buscar(String texto, Rol rol, Boolean activo, Pageable pageable) {
        return usuarioRepository.findAll(UsuarioSpecification.filtrar(texto, rol, activo), pageable);
    }

    public long contarTodos() {
        return usuarioRepository.count();
    }

    public long contarActivos() {
        return usuarioRepository.countByActivoTrue();
    }

    /**
     * Activa o desactiva una cuenta. Un usuario desactivado no puede loguearse.
     */
    @Transactional
    public Usuario cambiarEstado(Long id, boolean activo, String emailSolicitante) {
        Usuario usuario = obtenerPorId(id);
        if (!activo && usuario.getEmail().equals(normalizarEmail(emailSolicitante))) {
            throw new IllegalArgumentException("No podés desactivar tu propia cuenta");
        }
        usuario.setActivo(activo);
        log.info("Usuario {} {} por {}", usuario.getEmail(), activo ? "activado" : "desactivado", emailSolicitante);
        return usuario;
    }
}
