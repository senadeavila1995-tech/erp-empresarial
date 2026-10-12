-- ERP Empresarial
-- Migracion 007: categorias, clientes y relaciones opcionales.
-- Ejecutar una sola vez, despues de las migraciones 001-006.

CREATE TABLE categorias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,

    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uk_categorias_nombre UNIQUE (nombre),
    INDEX idx_categorias_activo_nombre (activo, nombre)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE clientes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo_identificacion VARCHAR(20) NULL,
    identificacion VARCHAR(50) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    telefono VARCHAR(40) NULL,
    email VARCHAR(180) NULL,
    direccion VARCHAR(300) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,

    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT uk_clientes_identificacion UNIQUE (identificacion),
    INDEX idx_clientes_activo_nombre (activo, nombre)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

ALTER TABLE productos
    ADD COLUMN categoria_id BIGINT NULL,
    ADD INDEX idx_productos_categoria (categoria_id),
    ADD CONSTRAINT fk_productos_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias(id)
        ON DELETE RESTRICT;

ALTER TABLE ventas
    ADD COLUMN cliente_id BIGINT NULL,
    ADD INDEX idx_ventas_cliente (cliente_id),
    ADD CONSTRAINT fk_ventas_cliente
        FOREIGN KEY (cliente_id) REFERENCES clientes(id)
        ON DELETE RESTRICT;
