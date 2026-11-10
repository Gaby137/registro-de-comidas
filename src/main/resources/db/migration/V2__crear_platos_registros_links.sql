-- =====================================================================
-- V2: platos, registros de comidas y link compartido con la nutricionista.
-- IDs: UUID como texto (36 caracteres). Fechas: siempre en UTC.
-- =====================================================================
CREATE TABLE platos (
    id          VARCHAR(36)  NOT NULL,
    nombre      VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    archivado   BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE registros (
    id         VARCHAR(36)  NOT NULL,
    fecha_hora DATETIME(6)  NOT NULL,
    foto_path  VARCHAR(255) NOT NULL,
    -- NULL = registro pendiente (foto cargada sin plato)
    plato_id   VARCHAR(36),
    nota       VARCHAR(500),
    creado_en  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    -- Sin ON DELETE: un plato con registros no se puede borrar, se archiva
    CONSTRAINT fk_registros_plato FOREIGN KEY (plato_id) REFERENCES platos (id)
);

CREATE INDEX idx_registros_fecha_hora ON registros (fecha_hora);

CREATE TABLE links_compartidos (
    id        VARCHAR(36) NOT NULL,
    token     VARCHAR(64) NOT NULL,
    activo    BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_links_compartidos_token UNIQUE (token)
);
