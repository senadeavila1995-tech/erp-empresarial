CREATE TABLE productos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    nombre VARCHAR(150) COLLATE utf8mb4_unicode_ci NOT NULL,
    descripcion VARCHAR(1000) COLLATE utf8mb4_unicode_ci NULL,
    precio_compra DECIMAL(12,2) NOT NULL,
    precio_venta DECIMAL(12,2) NOT NULL,
    stock_actual INT NOT NULL,
    stock_minimo INT NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,

    CONSTRAINT pk_productos PRIMARY KEY (id),
    CONSTRAINT uk_productos_sku UNIQUE (sku),
    CONSTRAINT chk_productos_precio_compra CHECK (precio_compra >= 0),
    CONSTRAINT chk_productos_precio_venta CHECK (precio_venta >= 0),
    CONSTRAINT chk_productos_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT chk_productos_stock_minimo CHECK (stock_minimo >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
