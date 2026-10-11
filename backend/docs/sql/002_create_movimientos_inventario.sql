CREATE TABLE movimientos_inventario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    producto_id BIGINT NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    cantidad INT NOT NULL,
    stock_anterior INT NOT NULL,
    stock_nuevo INT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    registrado_por VARCHAR(80) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_movimientos_inventario
        PRIMARY KEY (id),

    CONSTRAINT fk_movimientos_producto
        FOREIGN KEY (producto_id)
        REFERENCES productos(id),

    CONSTRAINT chk_movimientos_tipo
        CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE')),

    CONSTRAINT chk_movimientos_cantidad
        CHECK (
            (tipo = 'AJUSTE' AND cantidad >= 0)
            OR (tipo IN ('ENTRADA', 'SALIDA') AND cantidad > 0)
        ),

    CONSTRAINT chk_movimientos_stock_anterior
        CHECK (stock_anterior >= 0),

    CONSTRAINT chk_movimientos_stock_nuevo
        CHECK (stock_nuevo >= 0),

    INDEX idx_movimientos_producto_fecha (producto_id, creado_en),
    INDEX idx_movimientos_fecha (creado_en)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
