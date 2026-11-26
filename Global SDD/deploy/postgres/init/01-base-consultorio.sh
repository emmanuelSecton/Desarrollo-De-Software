#!/bin/bash
set -euo pipefail

slug_valido() {
  [[ "$1" =~ ^[a-z][a-z0-9_]{1,30}$ ]]
}

valor_slug() {
  local slug="$1"
  local sufijo="$2"
  local upper="${slug^^}"
  local variable="CONSULTORIO_${upper}_${sufijo}"
  echo "${!variable:-}"
}

crear_base_y_roles() {
  local db="$1"
  local migrator="$2"
  local runtime="$3"
  local migratepwd="$4"
  local runpwd="$5"

  if [ -z "$migratepwd" ] || [ -z "$runpwd" ]; then
    echo "ERROR: faltan passwords para $db" >&2
    exit 1
  fi

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v db="$db" \
    -v migrator="$migrator" -v migratepwd="$migratepwd" \
    -v runtime="$runtime" -v runpwd="$runpwd" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'migrator', :'migratepwd')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'migrator')\gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'runtime', :'runpwd')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'runtime')\gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'db', :'migrator')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db')\gexec
REVOKE CONNECT ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO :"runtime";
SQL

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" \
    -v runtime="$runtime" <<'SQL'
GRANT USAGE ON SCHEMA public TO :"runtime";
SQL
}

crear_instancia_por_slug() {
  local slug="$1"
  if ! slug_valido "$slug"; then
    echo "ERROR: slug invalido: $slug" >&2
    exit 1
  fi
  local base="consultorio_${slug}"
  local migrator="${base}_migrator"
  local runtime="${base}_runtime"
  local migratepwd
  local runpwd
  migratepwd="$(valor_slug "$slug" MIGRATOR_PASSWORD)"
  runpwd="$(valor_slug "$slug" RUNTIME_PASSWORD)"
  crear_base_y_roles "$base" "$migrator" "$runtime" \
    "${migratepwd:-${APP_DB_MIGRATOR_PASSWORD:-}}" \
    "${runpwd:-${APP_DB_PASSWORD:-}}"
}

slugs="${CONSULTORIO_SLUGS:-}"
primer_slug="${slugs%% *}"

crear_base_y_roles \
  "${APP_DB_NAME:-consultorio}" \
  "${APP_DB_MIGRATOR_USER:-app_migrator}" \
  "${APP_DB_USER:-app_runtime}" \
  "${APP_DB_MIGRATOR_PASSWORD:-$(valor_slug "$primer_slug" MIGRATOR_PASSWORD)}" \
  "${APP_DB_PASSWORD:-$(valor_slug "$primer_slug" RUNTIME_PASSWORD)}"

for slug in ${slugs}; do
  crear_instancia_por_slug "$slug"
done
