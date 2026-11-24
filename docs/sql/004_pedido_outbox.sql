-- Outbox de pedidos confirmados (solo hace falta con DDL_AUTO=validate; con update se crea sola).
CREATE TABLE IF NOT EXISTS pedido_outbox (
    id varchar(255) PRIMARY KEY, payload varchar(4000) NOT NULL,
    creado timestamptz, publicado timestamptz, intentos integer NOT NULL,
    proximo_intento timestamptz, ultimo_error varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_pedido_outbox_pendiente
    ON pedido_outbox (publicado, proximo_intento, creado);
