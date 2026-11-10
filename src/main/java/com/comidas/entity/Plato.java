package com.comidas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Plato precargado: lo que se elige en la selección rápida al cargar una comida.
 * Nunca se borra si tiene registros: se archiva.
 */
@Entity
@Table(name = "platos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plato {

    public static final int NOMBRE_MAX = 120;
    public static final int DESCRIPCION_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = NOMBRE_MAX)
    private String nombre;

    /** Corta: qué lleva y porción aproximada. */
    @Column(length = DESCRIPCION_MAX)
    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private boolean archivado = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant creadoEn;
}
