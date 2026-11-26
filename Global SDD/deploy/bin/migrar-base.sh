#!/bin/bash
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "uso: $0 <slug> [compose-generado]" >&2
  exit 1
fi

slug="$1"
argumentos=(-f deploy/docker-compose.yml)
if [ "$#" -eq 2 ]; then
  if [ ! -f "$2" ]; then
    echo "ERROR: no existe el compose generado $2" >&2
    exit 1
  fi
  argumentos+=(-f "$2")
fi
configuracion=$(docker compose "${argumentos[@]}" config --format json)

for texto in "flyway/flyway:11.7.2" "FLYWAY_PLACEHOLDERS_RUNTIME_ROLE" "APP_DB_USER" "service_completed_successfully"; do
  if ! echo "$configuracion" | grep -q "$texto"; then
    echo "ERROR: la configuracion no contiene $texto" >&2
    exit 1
  fi
done

docker compose "${argumentos[@]}" run --rm "migrate_${slug}"
docker compose "${argumentos[@]}" up -d --no-build "app_${slug}"
