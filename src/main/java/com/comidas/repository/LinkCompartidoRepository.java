package com.comidas.repository;

import com.comidas.entity.LinkCompartido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkCompartidoRepository extends JpaRepository<LinkCompartido, UUID> {

    List<LinkCompartido> findByActivoTrue();

    Optional<LinkCompartido> findFirstByActivoTrueOrderByCreadoEnDesc();

    boolean existsByTokenAndActivoTrue(String token);
}
