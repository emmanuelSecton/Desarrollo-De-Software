-- Init script SOLO para Testcontainers (C-02, D2): crea el rol runtime sobre la base del contenedor.
-- Password ficticia y explicita de prueba; nunca se usa en despliegue.
CREATE ROLE app_runtime LOGIN PASSWORD 'clave-ficticia-de-prueba-runtime';

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_runtime', current_database());
END
$$;

GRANT USAGE ON SCHEMA public TO app_runtime;
