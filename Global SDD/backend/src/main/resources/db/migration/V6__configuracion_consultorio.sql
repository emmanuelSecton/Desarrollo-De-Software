-- V6: configuracion del consultorio, horarios de atencion y prestaciones (C-05).
-- La base garantiza la coherencia (singleton, franjas sin superposicion, duraciones validas), no solo la API.

-- Un solo consultorio por instancia: "unico" solo puede valer TRUE y es UNIQUE (D3).
CREATE TABLE consultorio (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unico                       BOOLEAN NOT NULL DEFAULT TRUE,
    nombre                      VARCHAR(120) NOT NULL,
    profesional_nombre          VARCHAR(120) NOT NULL,
    matricula                   VARCHAR(30),
    direccion                   VARCHAR(200),
    telefono                    VARCHAR(30),
    email                       CITEXT,
    slug_reserva                VARCHAR(60) NOT NULL,
    recordatorio_horas_antes    INT NOT NULL DEFAULT 48,
    confirmacion_limite_horas   INT NOT NULL DEFAULT 24,
    cancelacion_minima_horas    INT NOT NULL DEFAULT 24,
    oferta_vencimiento_minutos  INT NOT NULL DEFAULT 30,
    actualizado_at              TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_consultorio_unico UNIQUE (unico),
    CONSTRAINT ck_consultorio_unico CHECK (unico),
    CONSTRAINT uq_consultorio_slug UNIQUE (slug_reserva),
    CONSTRAINT ck_consultorio_slug CHECK (slug_reserva ~ '^[a-z0-9-]{3,60}$'),
    CONSTRAINT ck_consultorio_recordatorio CHECK (recordatorio_horas_antes BETWEEN 1 AND 168),
    CONSTRAINT ck_consultorio_confirmacion
        CHECK (confirmacion_limite_horas >= 1 AND confirmacion_limite_horas < recordatorio_horas_antes),
    CONSTRAINT ck_consultorio_cancelacion CHECK (cancelacion_minima_horas BETWEEN 0 AND 168),
    CONSTRAINT ck_consultorio_oferta CHECK (oferta_vencimiento_minutos BETWEEN 5 AND 1440)
);

-- Franjas por dia (1 = lunes ... 7 = domingo), en multiplos de 5 minutos y sin superposicion en el mismo dia (D4).
-- El rango [) acepta franjas contiguas (09:00-13:00 y 13:00-14:00).
CREATE TABLE horario_atencion (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dia_semana  SMALLINT NOT NULL,
    hora_desde  TIME NOT NULL,
    hora_hasta  TIME NOT NULL,
    CONSTRAINT ck_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_orden CHECK (hora_desde < hora_hasta),
    CONSTRAINT ck_horario_multiplo_5 CHECK (
        EXTRACT(MINUTE FROM hora_desde)::INT % 5 = 0 AND EXTRACT(SECOND FROM hora_desde) = 0
        AND EXTRACT(MINUTE FROM hora_hasta)::INT % 5 = 0 AND EXTRACT(SECOND FROM hora_hasta) = 0),
    CONSTRAINT ex_horario_sin_superposicion EXCLUDE USING gist (
        dia_semana WITH =,
        tsrange(DATE '2000-01-01' + hora_desde, DATE '2000-01-01' + hora_hasta) WITH &&)
);

CREATE INDEX idx_horario_dia ON horario_atencion (dia_semana, hora_desde);

-- Prestaciones: duracion multiplo de 5 (RN-AG-02), hasta 8 h, y nunca se borran: baja logica (D5).
CREATE TABLE prestacion (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre             VARCHAR(80) NOT NULL,
    duracion_minutos   INT NOT NULL,
    precio_referencia  NUMERIC(12, 2),
    reservable_online  BOOLEAN NOT NULL DEFAULT TRUE,
    activa             BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_prestacion_duracion CHECK (duracion_minutos > 0 AND duracion_minutos % 5 = 0 AND duracion_minutos <= 480),
    CONSTRAINT ck_prestacion_precio CHECK (precio_referencia IS NULL OR precio_referencia >= 0)
);

-- Nombre unico entre las activas, sin distinguir mayusculas.
CREATE UNIQUE INDEX uq_prestacion_nombre_activa ON prestacion (lower(nombre)) WHERE activa;

-- Datos iniciales (D6). Flyway los inserta una sola vez por base: lo que el odontologo borre no reaparece.
INSERT INTO consultorio (nombre, profesional_nombre, slug_reserva, actualizado_at)
VALUES ('Mi consultorio', 'Profesional (completar)', 'mi-consultorio', now());

INSERT INTO horario_atencion (dia_semana, hora_desde, hora_hasta)
SELECT dia, franja.desde, franja.hasta
FROM generate_series(1, 5) AS dia
CROSS JOIN (VALUES (TIME '09:00', TIME '13:00'), (TIME '15:00', TIME '19:00')) AS franja (desde, hasta);

INSERT INTO prestacion (nombre, duracion_minutos) VALUES
    ('Consulta/diagnóstico', 30),
    ('Limpieza', 45),
    ('Obturación', 45),
    ('Endodoncia', 90),
    ('Extracción', 45),
    ('Control', 15);

-- El runtime no crea ni borra el consultorio y no borra prestaciones; las franjas si se borran.
GRANT SELECT, UPDATE ON consultorio TO ${runtime_role};
GRANT SELECT, INSERT, UPDATE, DELETE ON horario_atencion TO ${runtime_role};
GRANT SELECT, INSERT, UPDATE ON prestacion TO ${runtime_role};
