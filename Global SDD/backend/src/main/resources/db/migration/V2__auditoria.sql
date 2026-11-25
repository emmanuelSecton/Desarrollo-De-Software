-- V2: auditoria append-only (C-02). Sin enum de base para accion/actor_tipo (D1).
-- ocurrido_at NO tiene DEFAULT: viene del Clock inyectado (RN-GL-02).
CREATE TABLE evento_auditoria (
    id          BIGSERIAL PRIMARY KEY,
    ocurrido_at TIMESTAMPTZ NOT NULL,
    actor_tipo  VARCHAR(20) NOT NULL,
    actor_id    VARCHAR(100),
    accion      VARCHAR(60) NOT NULL,
    entidad     VARCHAR(80) NOT NULL,
    entidad_id  VARCHAR(100) NOT NULL,
    detalle     JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip          INET,
    CONSTRAINT ck_evento_auditoria_actor CHECK (
        (actor_tipo = 'SISTEMA' AND actor_id IS NULL)
        OR (actor_tipo IN ('USUARIO', 'PACIENTE') AND actor_id IS NOT NULL)
    ),
    CONSTRAINT ck_evento_auditoria_detalle_objeto CHECK (jsonb_typeof(detalle) = 'object')
);

CREATE OR REPLACE FUNCTION rechazar_modificacion_evento_auditoria() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'evento_auditoria es append-only: UPDATE/DELETE no permitidos'
        USING ERRCODE = '42501';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_evento_auditoria_append_only
    BEFORE UPDATE OR DELETE ON evento_auditoria
    FOR EACH ROW EXECUTE FUNCTION rechazar_modificacion_evento_auditoria();

CREATE INDEX idx_evento_auditoria_ocurrido_at ON evento_auditoria (ocurrido_at DESC);
CREATE INDEX idx_evento_auditoria_actor ON evento_auditoria (actor_tipo, actor_id);
CREATE INDEX idx_evento_auditoria_accion ON evento_auditoria (accion);
CREATE INDEX idx_evento_auditoria_entidad ON evento_auditoria (entidad, entidad_id);

GRANT USAGE, SELECT ON SEQUENCE evento_auditoria_id_seq TO app_runtime;
GRANT SELECT, INSERT ON evento_auditoria TO app_runtime;
