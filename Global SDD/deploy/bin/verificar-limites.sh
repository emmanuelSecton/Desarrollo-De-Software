#!/bin/bash
set -euo pipefail

configuracion=$(docker compose -f deploy/docker-compose.yml config --format json)

for servicio in app_demo_uno app_demo_dos postgres migrate_demo_uno migrate_demo_dos caddy; do
  if ! echo "$configuracion" | grep -q "\"${servicio}\""; then
    echo "ERROR: falta el servicio ${servicio}" >&2
    exit 1
  fi
done

if ! echo "$configuracion" | grep -q '"cpus"'; then
  echo "ERROR: faltan limites de CPU" >&2
  exit 1
fi
if ! echo "$configuracion" | grep -q '"mem_limit"'; then
  echo "ERROR: faltan limites de memoria" >&2
  exit 1
fi
if ! echo "$configuracion" | grep -q '"DB_POOL_MAX_SIZE": "5"'; then
  echo "ERROR: el pool debe ser 5 o menos" >&2
  exit 1
fi

echo "limites verificados: cpus, mem_limit y DB_POOL_MAX_SIZE=5"
