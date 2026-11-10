-- =====================================================================
-- V1: tabla de usuarios (autenticación MVC + API)
-- Convención: V<n>__descripcion.sql — nunca editar una migración ya aplicada,
-- siempre crear una nueva (V2__..., V3__...).
-- =====================================================================
CREATE TABLE usuarios (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(50)  NOT NULL,
    apellido       VARCHAR(50)  NOT NULL,
    email          VARCHAR(150) NOT NULL,
    contrasena     VARCHAR(100) NOT NULL,
    telefono       VARCHAR(30),
    rol            VARCHAR(20)  NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      DATETIME(6),
    actualizado_en DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);
