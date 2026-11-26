-- Trazas del asistente de IA (solo hace falta con DDL_AUTO=validate; con update se crea sola).
-- No guarda el texto enviado ni el recibido, ni quién hizo la solicitud.
CREATE TABLE IF NOT EXISTS ia_traza (
    id bigserial PRIMARY KEY,
    creada timestamptz,
    funcionalidad varchar(60),
    version_prompt varchar(60),
    proveedor varchar(40),
    modelo varchar(80),
    resultado varchar(30),
    latencia_ms bigint NOT NULL,
    tokens_entrada integer NOT NULL,
    tokens_salida integer NOT NULL,
    costo_estimado_usd double precision NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ia_traza_creada ON ia_traza (creada);
