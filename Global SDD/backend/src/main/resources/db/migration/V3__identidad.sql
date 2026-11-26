-- V3: usuarios internos (C-03). Rol con CHECK en vez de ENUM de PostgreSQL (D1).
-- creado_at NO tiene DEFAULT: viene del Clock inyectado (RN-GL-02).
CREATE TABLE usuario (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 CITEXT NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    nombre                VARCHAR(120) NOT NULL,
    rol                   VARCHAR(20) NOT NULL,
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    debe_cambiar_password BOOLEAN NOT NULL DEFAULT TRUE,
    intentos_fallidos     INT NOT NULL DEFAULT 0,
    primer_fallo_at       TIMESTAMPTZ,
    bloqueado_hasta       TIMESTAMPTZ,
    ultimo_login_at       TIMESTAMPTZ,
    creado_at             TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ODONTOLOGO', 'SECRETARIA')),
    CONSTRAINT ck_usuario_intentos CHECK (intentos_fallidos >= 0)
);

-- Como maximo un ODONTOLOGO activo. Que haya al menos uno lo garantizan el alta inicial y C-05.
CREATE UNIQUE INDEX uq_usuario_odontologo_activo ON usuario ((rol)) WHERE rol = 'ODONTOLOGO' AND activo;

-- Sin DELETE: la baja es logica (activo = false).
GRANT SELECT, INSERT, UPDATE ON usuario TO ${runtime_role};
