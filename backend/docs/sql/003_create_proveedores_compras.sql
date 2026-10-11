CREATE TABLE IF NOT EXISTS proveedores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo_identificacion VARCHAR(20) NULL,
    identificacion VARCHAR(50) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    contacto VARCHAR(120) NULL,
    telefono VARCHAR(40) NULL,
    email VARCHAR(180) NULL,
    direccion VARCHAR(300) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_proveedores_identificacion UNIQUE (identificacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS compras (
    id BIGINT NOT NULL AUTO_INCREMENT,
    proveedor_id BIGINT NOT NULL,
    numero_factura VARCHAR(100) NOT NULL,
    fecha_compra DATE NOT NULL,
    subtotal DECIMAL(14,2) NOT NULL,
    estado VARCHAR(15) NOT NULL,
    registrado_por VARCHAR(80) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_compras_proveedor_factura UNIQUE (proveedor_id, numero_factura),
    CONSTRAINT fk_compras_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    CONSTRAINT chk_compras_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_compras_estado CHECK (estado IN ('CONFIRMADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS detalle_compras (
    id BIGINT NOT NULL AUTO_INCREMENT,
    compra_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    costo_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_detalle_compra_producto UNIQUE (compra_id, producto_id),
    CONSTRAINT fk_detalle_compra FOREIGN KEY (compra_id) REFERENCES compras(id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_costo CHECK (costo_unitario >= 0),
    CONSTRAINT chk_detalle_subtotal CHECK (subtotal >= 0),
    INDEX idx_detalle_producto (producto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
