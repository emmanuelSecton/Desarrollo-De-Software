#!/bin/bash
# Se ejecuta una sola vez, al crear el volumen de PostgreSQL (DD-01).
# Crea dos roles: app_migrator (dueno de la base, ejecuta Flyway/DDL) y
# app_runtime (la app en operacion: CONNECT y USAGE, sin DDL ni escritura destructiva).
# La app nunca usa el superusuario. C-35 lo extiende a una base por consultorio.
set -euo pipefail

for variable in APP_DB_NAME APP_DB_MIGRATOR_USER APP_DB_MIGRATOR_PASSWORD APP_DB_USER APP_DB_PASSWORD; do
  if [ -z "${!variable:-}" ]; then
    echo "ERROR: falta la variable $variable (ver .env.example)" >&2
    exit 1
  fi
done

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v db="$APP_DB_NAME" \
  -v migrator="$APP_DB_MIGRATOR_USER" -v migratepwd="$APP_DB_MIGRATOR_PASSWORD" \
  -v runtime="$APP_DB_USER" -v runpwd="$APP_DB_PASSWORD" <<'SQL'
CREATE ROLE :"migrator" LOGIN PASSWORD :'migratepwd';
CREATE ROLE :"runtime" LOGIN PASSWORD :'runpwd';
CREATE DATABASE :"db" OWNER :"migrator";
REVOKE CONNECT ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO :"runtime";
SQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$APP_DB_NAME" \
  -v runtime="$APP_DB_USER" <<'SQL'
GRANT USAGE ON SCHEMA public TO :"runtime";
SQL
