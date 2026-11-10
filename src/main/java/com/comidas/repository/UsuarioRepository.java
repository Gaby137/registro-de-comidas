package com.comidas.repository;

import com.comidas.entity.Usuario;
import com.comidas.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repositorio de usuarios.
 * Extiende JpaSpecificationExecutor para soportar filtros dinámicos con Specification.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long>,
                                           JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRol(Rol rol);

    long countByActivoTrue();
}
