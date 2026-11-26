#!/bin/bash
set -euo pipefail

if [ -z "${CONSULTORIO_SLUGS:-}" ] || [ -z "${ODONTO_BACKUP_DIR:-}" ]; then
  echo "ERROR: faltan CONSULTORIO_SLUGS u ODONTO_BACKUP_DIR" >&2
  exit 1
fi

jar="${ODONTO_JAR:-../backend/build/libs/app.jar}"
if [ ! -f "$jar" ]; then
  echo "ERROR: no existe $jar (ejecutar ./gradlew.bat bootJar)" >&2
  exit 1
fi

mkdir -p "$ODONTO_BACKUP_DIR"
fecha="$(date -u +%Y-%m-%dT%H%M%SZ)"

for slug in ${CONSULTORIO_SLUGS}; do
  base="consultorio_${slug}"
  archivo="${ODONTO_BACKUP_DIR}/${base}-${fecha}.dump"
  if ! docker compose -f deploy/docker-compose.yml exec -T postgres \
      pg_dump -U "${POSTGRES_USER:-odonto}" -d "$base" -Fc > "$archivo"; then
    java -cp "$jar" ar.edu.utn.odonto.operacion.BackupsDiarios "$base"
  fi
  echo "backup de ${base} en ${archivo}"
done

java -cp "$jar" ar.edu.utn.odonto.operacion.RetencionBackups \
  "$ODONTO_BACKUP_DIR" "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "retencion aplicada: 30 dias"
