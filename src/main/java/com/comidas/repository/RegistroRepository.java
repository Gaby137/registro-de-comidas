package com.comidas.repository;

import com.comidas.entity.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Las consultas traen el plato con join fetch: open-in-view está apagado
 * y las vistas se arman fuera de la transacción.
 */
public interface RegistroRepository extends JpaRepository<Registro, UUID> {

    @Query("select r from Registro r left join fetch r.plato where r.id = :id")
    Optional<Registro> buscarConPlato(@Param("id") UUID id);

    /** Registros de un rango [desde, hasta), del más temprano al más tardío. */
    @Query("""
            select r from Registro r left join fetch r.plato
            where r.fechaHora >= :desde and r.fechaHora < :hasta
            order by r.fechaHora asc, r.creadoEn asc
            """)
    List<Registro> buscarEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /** Registros sin plato, del más reciente al más viejo. */
    @Query("select r from Registro r where r.plato is null order by r.fechaHora desc")
    List<Registro> buscarPendientes();

    /** Registros ya identificados desde una fecha: base del orden de la selección rápida. */
    @Query("select r from Registro r join fetch r.plato where r.fechaHora >= :desde")
    List<Registro> buscarConPlatoDesde(@Param("desde") Instant desde);
}
