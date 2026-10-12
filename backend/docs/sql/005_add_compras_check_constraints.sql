-- Migracion incremental: restricciones CHECK para compras.
-- Requiere MySQL 8.0.16 o posterior.
-- Verificar previamente que estas restricciones no existan.

ALTER TABLE compras
    ADD CONSTRAINT chk_compras_subtotal
        CHECK (subtotal >= 0),
    ADD CONSTRAINT chk_compras_estado
        CHECK (estado IN ('CONFIRMADA'));

ALTER TABLE detalle_compras
    ADD CONSTRAINT chk_detalle_cantidad
        CHECK (cantidad > 0),
    ADD CONSTRAINT chk_detalle_costo
        CHECK (costo_unitario >= 0),
    ADD CONSTRAINT chk_detalle_subtotal
        CHECK (subtotal >= 0);
