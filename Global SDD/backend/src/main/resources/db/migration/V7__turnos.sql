-- V7: turnos (C-10). La garantia de no superposicion la da la base, nunca solo Java (RN-AG-01, regla dura).
-- Las fechas vienen del Clock inyectado (RN-GL-02); estados y origen con CHECK, igual que C-02/C-03/C-05.
CREATE TABLE turno (
    id                      UUID PRIMARY KEY,
    paciente_id             UUID NOT NULL REFERENCES paciente (id),
    prestacion_id           UUID NOT NULL REFERENCES prestacion (id),
    inicio                  TIMESTAMPTZ NOT NULL,
    fin                     TIMESTAMPTZ NOT NULL,
    estado                  VARCHAR(12) NOT NULL,
    origen                  VARCHAR(8) NOT NULL,
    sobreturno              BOOLEAN NOT NULL DEFAULT FALSE,
    reprogramado_de         UUID REFERENCES turno (id),
    token_publico           CHAR(43) NOT NULL,
    sin_confirmar_alertado  BOOLEAN NOT NULL DEFAULT FALSE,
    google_event_id         VARCHAR(255),
    cancelado_por           VARCHAR(12),
    motivo_cancelacion      VARCHAR(200),
    creado_at               TIMESTAMPTZ NOT NULL,
    actualizado_at          TIMESTAMPTZ NOT NULL,
    version                 INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_turno_intervalo CHECK (fin > inicio),
    CONSTRAINT ck_turno_estado CHECK (estado IN ('RESERVADO', 'CONFIRMADO', 'CANCELADO', 'ATENDIDO', 'AUSENTE')),
    CONSTRAINT ck_turno_origen CHECK (origen IN ('INTERNO', 'ONLINE')),
    CONSTRAINT ck_turno_cancelado_por CHECK (cancelado_por IN ('PACIENTE', 'CONSULTORIO')),
    -- Un turno cancelado siempre dice quien cancelo y por que; uno no cancelado no lo dice.
    CONSTRAINT ck_turno_cancelacion CHECK (
        (estado = 'CANCELADO') = (cancelado_por IS NOT NULL AND motivo_cancelacion IS NOT NULL)),
    CONSTRAINT uq_turno_token UNIQUE (token_publico),
    -- Anti-solapamiento (RN-AG-01, RN-AG-07): dos turnos activos no se superponen, salvo sobreturnos.
    -- El rango [) acepta turnos contiguos.
    CONSTRAINT ex_turno_sin_superposicion EXCLUDE USING gist (tstzrange(inicio, fin) WITH &&)
        WHERE (estado IN ('RESERVADO', 'CONFIRMADO') AND NOT sobreturno)
);

-- Un turno se reprograma una sola vez.
CREATE UNIQUE INDEX uq_turno_reprogramado_de ON turno (reprogramado_de) WHERE reprogramado_de IS NOT NULL;
CREATE INDEX idx_turno_inicio ON turno (inicio);
CREATE INDEX idx_turno_estado_inicio ON turno (estado, inicio);
CREATE INDEX idx_turno_paciente ON turno (paciente_id);

-- Sin DELETE: un turno se cancela, nunca se borra.
GRANT SELECT, INSERT, UPDATE ON turno TO ${runtime_role};
