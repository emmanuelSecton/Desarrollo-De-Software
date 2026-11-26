#!/bin/bash
set -euo pipefail

jar="${ODONTO_JAR:-../backend/build/libs/app.jar}"
if [ ! -f "$jar" ]; then
  echo "ERROR: no existe $jar (ejecutar ./gradlew.bat bootJar)" >&2
  exit 1
fi

if [ -z "${CONSULTORIO_SLUGS:-}" ]; then
  echo "ERROR: falta CONSULTORIO_SLUGS para migrar-base.sh" >&2
  exit 1
fi

java -cp "$jar" ar.edu.utn.odonto.operacion.MigradorTodas ${CONSULTORIO_SLUGS}
