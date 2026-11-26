-- V5: tokens de recuperacion de contrasena (C-04, D3). Solo se guarda el SHA-256 del token;
-- vence a la hora y sirve una sola vez (usado_at). Las fechas vienen del Clock inyectado.
CREATE TABLE token_recuperacion (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario (id),
    token_hash  CHAR(64) NOT NULL,
    creado_at   TIMESTAMPTZ NOT NULL,
    vence_at    TIMESTAMPTZ NOT NULL,
    usado_at    TIMESTAMPTZ,
    CONSTRAINT uq_token_recuperacion_hash UNIQUE (token_hash),
    CONSTRAINT ck_token_recuperacion_vencimiento CHECK (vence_at > creado_at)
);

CREATE INDEX idx_token_recuperacion_usuario ON token_recuperacion (usuario_id);

-- Sin DELETE: los tokens usados o invalidados quedan como historial.
GRANT SELECT, INSERT, UPDATE ON token_recuperacion TO ${runtime_role};
