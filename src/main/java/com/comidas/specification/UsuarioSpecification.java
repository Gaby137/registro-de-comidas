package com.comidas.specification;

import com.comidas.entity.Usuario;
import com.comidas.enums.Rol;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Specifications para construir consultas dinámicas sobre la entidad Usuario.
 * <p>
 * Es el mecanismo estándar de Spring Data JPA para filtros combinables.
 * <p>
 * Uso en un Service:
 * <pre>{@code
 * var spec = Specification.allOf(UsuarioSpecification.tieneRol(Rol.ADMIN),
 *                                UsuarioSpecification.estaActivo());
 * Page<Usuario> resultados = usuarioRepository.findAll(spec, pageable);
 * }</pre>
 */
public final class UsuarioSpecification {

    private UsuarioSpecification() {
        // Utility class
    }

    /**
     * Combina los filtros que vengan informados; los null/blank se ignoran.
     * Patrón recomendado para formularios de búsqueda y query params de la API.
     */
    public static Specification<Usuario> filtrar(String texto, Rol rol, Boolean activo) {
        Specification<Usuario> spec = Specification.unrestricted();
        if (texto != null && !texto.isBlank()) {
            spec = spec.and(nombreContiene(texto).or(apellidoContiene(texto)).or(emailContiene(texto)));
        }
        if (rol != null) {
            spec = spec.and(tieneRol(rol));
        }
        if (activo != null) {
            spec = spec.and(activo ? estaActivo() : estaInactivo());
        }
        return spec;
    }

    public static Specification<Usuario> tieneRol(Rol rol) {
        return (root, query, cb) -> cb.equal(root.get("rol"), rol);
    }

    public static Specification<Usuario> nombreContiene(String nombre) {
        return contiene("nombre", nombre);
    }

    public static Specification<Usuario> apellidoContiene(String apellido) {
        return contiene("apellido", apellido);
    }

    public static Specification<Usuario> emailContiene(String email) {
        return contiene("email", email);
    }

    public static Specification<Usuario> estaActivo() {
        return (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    public static Specification<Usuario> estaInactivo() {
        return (root, query, cb) -> cb.isFalse(root.get("activo"));
    }

    /**
     * LIKE case-insensitive escapando los comodines (%, _) que escriba el usuario.
     */
    private static Specification<Usuario> contiene(String campo, String valor) {
        String patron = "%" + escaparLike(valor.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(campo)), patron, '\\');
    }

    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
