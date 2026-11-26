#!/bin/bash
set -euo pipefail

if [ -z "${CONSULTORIO_SLUGS:-}" ]; then
  echo "ERROR: falta CONSULTORIO_SLUGS" >&2
  exit 1
fi

fallos=0
for slug_actual in ${CONSULTORIO_SLUGS}; do
  base_actual="consultorio_${slug_actual}"
  runtime_actual="${base_actual}_runtime"
  for slug_ajeno in ${CONSULTORIO_SLUGS}; do
    if [ "$slug_actual" = "$slug_ajeno" ]; then
      continue
    fi
    base_ajena="consultorio_${slug_ajeno}"
    propio=$(docker compose exec -T postgres psql -U postgres -d postgres -Atc \
      "select has_database_privilege('${runtime_actual}', '${base_actual}', 'CONNECT')::text")
    ajeno=$(docker compose exec -T postgres psql -U postgres -d postgres -Atc \
      "select has_database_privilege('${runtime_actual}', '${base_ajena}', 'CONNECT')::text")
    publico=$(docker compose exec -T postgres psql -U postgres -d postgres -Atc \
      "select has_database_privilege('public', '${base_actual}', 'CONNECT')::text")
    if [ "$propio" != "true" ] || [ "$ajeno" != "false" ] || [ "$publico" != "false" ]; then
      echo "FALLO de aislamiento: ${runtime_actual} propio=${propio} ajeno=${ajeno} public=${publico}" >&2
      fallos=$((fallos + 1))
    fi
  done
done

if [ "$fallos" -ne 0 ]; then
  exit 1
fi
echo "aislamiento verificado para ${CONSULTORIO_SLUGS}"
