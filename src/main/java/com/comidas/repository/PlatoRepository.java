package com.comidas.repository;

import com.comidas.entity.Plato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlatoRepository extends JpaRepository<Plato, UUID> {

    List<Plato> findByArchivadoFalse();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);
}
