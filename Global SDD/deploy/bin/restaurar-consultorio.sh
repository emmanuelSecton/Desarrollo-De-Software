#!/bin/bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "uso: $0 <slug> <archivo.dump>" >&2
  exit 1
fi

slug="$1"
archivo="$2"
base="consultorio_${slug}"

case " ${CONSULTORIO_SLUGS:-} " in
  *" $slug "*) ;;
  *) echo "ERROR: el slug no figura en CONSULTORIO_SLUGS" >&2; exit 1 ;;
esac

docker compose -f deploy/docker-compose.yml exec -T postgres \
  pg_restore -U "${POSTGRES_USER:-odonto}" -d "$base" --clean --if-exists --exit-on-error "$archivo"

docker compose -f deploy/docker-compose.yml exec -T postgres \
  psql -U "${POSTGRES_USER:-odonto}" -d "$base" -v ON_ERROR_STOP=1 \
  -c "REVOKE CONNECT ON DATABASE \"$base\" FROM PUBLIC" \
  -c "GRANT CONNECT ON DATABASE \"$base\" TO consultorio_${slug}_runtime" \
  -c "GRANT USAGE ON SCHEMA public TO consultorio_${slug}_runtime" \
  -c "GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA public TO consultorio_${slug}_runtime" \
  -c "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO consultorio_${slug}_runtime" \
  -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT ON TABLES TO consultorio_${slug}_runtime" \
  -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO consultorio_${slug}_runtime"

echo "restaurada la base ${base} del slug ${slug}; verificar health"
