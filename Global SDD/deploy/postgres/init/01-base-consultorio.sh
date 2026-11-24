#!/bin/bash
# Se ejecuta una sola vez, al crear el volumen de PostgreSQL (DD-01).
# Crea la base y el usuario de la instancia. La app nunca usa el superusuario.
# C-35 lo extiende a una base por consultorio.
set -euo pipefail

for variable in APP_DB_NAME APP_DB_USER APP_DB_PASSWORD; do
  if [ -z "${!variable:-}" ]; then
    echo "ERROR: falta la variable $variable (ver .env.example)" >&2
    exit 1
  fi
done

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v db="$APP_DB_NAME" -v usr="$APP_DB_USER" -v pwd="$APP_DB_PASSWORD" <<'SQL'
CREATE ROLE :"usr" LOGIN PASSWORD :'pwd';
CREATE DATABASE :"db" OWNER :"usr";
REVOKE CONNECT ON DATABASE :"db" FROM PUBLIC;
SQL
