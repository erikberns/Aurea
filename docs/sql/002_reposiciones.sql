-- Agrega tablas sin eliminar datos. Aplicar antes de iniciar con DDL_AUTO=validate.
CREATE TABLE IF NOT EXISTS reposicion (
    id varchar(255) PRIMARY KEY, producto_id bigint NOT NULL,
    codigo_proveedor varchar(255) NOT NULL, cantidad integer NOT NULL,
    estado varchar(255) NOT NULL, creada timestamptz, recibida timestamptz,
    detalle varchar(500), version bigint
);
CREATE TABLE IF NOT EXISTS reposicion_outbox (
    id varchar(255) PRIMARY KEY, payload varchar(4000) NOT NULL,
    creado timestamptz, publicado timestamptz, intentos integer NOT NULL,
    proximo_intento timestamptz, ultimo_error varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_reposicion_outbox_pendiente
    ON reposicion_outbox (publicado, proximo_intento, creado);
CREATE TABLE IF NOT EXISTS inventario_reposicion_aplicada (
    id varchar(255) PRIMARY KEY, producto_id bigint, cantidad integer NOT NULL, aplicada timestamptz
);
CREATE TABLE IF NOT EXISTS notificacion_administrativa (
    id varchar(255) PRIMARY KEY, mensaje varchar(255), creada timestamptz
);
