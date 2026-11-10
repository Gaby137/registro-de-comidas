package com.comidas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Superclase base para entidades auditables.
 * Agrega automáticamente campos de creación y última modificación.
 * <p>
 * Usa @CreationTimestamp / @UpdateTimestamp de Hibernate: no hace falta setearlos a mano.
 * <p>
 * Uso: hacer que tus entidades extiendan esta clase.
 * <pre>{@code
 * @Entity
 * public class Producto extends BaseAuditable { ... }
 * }</pre>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseAuditable {

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    private LocalDateTime actualizadoEn;
}
