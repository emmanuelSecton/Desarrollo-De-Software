-- V4: pacientes (C-09). Nunca se borran fisicamente (RN-HC-03): baja logica con "activo".
-- Las fechas NO tienen DEFAULT: vienen del Clock inyectado (RN-GL-02).
-- unaccent es una extension "trusted" (PostgreSQL 13+): la crea el migrator, duenio de la base.
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE paciente (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dni                         VARCHAR(10) NOT NULL,
    nombre                      VARCHAR(80) NOT NULL,
    apellido                    VARCHAR(80) NOT NULL,
    fecha_nacimiento            DATE,
    telefono_e164               VARCHAR(16) NOT NULL,
    email                       CITEXT,
    consentimiento_datos_at     TIMESTAMPTZ NOT NULL,
    politica_privacidad_version VARCHAR(20) NOT NULL,
    acepta_whatsapp             BOOLEAN NOT NULL DEFAULT FALSE,
    whatsapp_consentimiento_at  TIMESTAMPTZ,
    ausencias_count             INT NOT NULL DEFAULT 0,
    activo                      BOOLEAN NOT NULL DEFAULT TRUE,
    creado_at                   TIMESTAMPTZ NOT NULL,
    actualizado_at              TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_paciente_dni UNIQUE (dni),
    CONSTRAINT ck_paciente_dni CHECK (dni ~ '^[0-9]{7,9}$'),
    CONSTRAINT ck_paciente_telefono CHECK (telefono_e164 ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT ck_paciente_ausencias CHECK (ausencias_count >= 0),
    CONSTRAINT ck_paciente_whatsapp CHECK (acepta_whatsapp = (whatsapp_consentimiento_at IS NOT NULL))
);

CREATE INDEX idx_paciente_apellido_nombre ON paciente (apellido, nombre);
CREATE INDEX idx_paciente_telefono ON paciente (telefono_e164);

-- Sin DELETE: la baja es logica.
GRANT SELECT, INSERT, UPDATE ON paciente TO ${runtime_role};
