-- Agrega el estado de activación a los usuarios existentes.
-- Las cuentas actuales permanecen activas.
-- Ejecutar una sola vez en la base de datos erp_empresarial.

ALTER TABLE usuarios
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
