CREATE TABLE IF NOT EXISTS ventas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    numero_factura VARCHAR(100) NOT NULL,
    fecha_venta DATE NOT NULL,
    subtotal DECIMAL(14,2) NOT NULL,
    total DECIMAL(14,2) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'CONFIRMADA',
    registrado_por VARCHAR(80) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_ventas_numero_factura UNIQUE (numero_factura),
    CONSTRAINT chk_ventas_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_ventas_total CHECK (total >= 0),
    CONSTRAINT chk_ventas_estado CHECK (estado IN ('CONFIRMADA')),
    INDEX idx_ventas_fecha (fecha_venta),
    INDEX idx_ventas_estado_fecha (estado, fecha_venta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS detalle_ventas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    venta_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_detalle_venta_producto UNIQUE (venta_id, producto_id),
    CONSTRAINT fk_detalle_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_detalle_venta_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT chk_detalle_venta_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_venta_precio CHECK (precio_unitario >= 0),
    CONSTRAINT chk_detalle_venta_subtotal CHECK (subtotal >= 0),
    INDEX idx_detalle_ventas_producto (producto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
