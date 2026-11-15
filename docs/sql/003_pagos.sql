-- Componente Pagos. Aplicar antes de iniciar con DDL_AUTO=validate. No modifica datos existentes.
CREATE TABLE IF NOT EXISTS pago (
    id bigserial PRIMARY KEY,
    orden_id bigint NOT NULL,
    proveedor varchar(255) NOT NULL,
    referencia_externa varchar(255),
    metodo varchar(255) NOT NULL,
    monto_centavos bigint NOT NULL,
    moneda varchar(3) NOT NULL,
    estado varchar(255) NOT NULL,
    detalle varchar(500),
    creado timestamptz,
    actualizado timestamptz,
    version bigint
);
CREATE INDEX IF NOT EXISTS idx_pago_orden ON pago (orden_id);
