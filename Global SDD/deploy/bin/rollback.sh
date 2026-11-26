#!/bin/bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "uso: $0 <slug>" >&2
  exit 1
fi

slug="$1"
jar="${ODONTO_JAR:-../backend/build/libs/app.jar}"

if [ ! -f "$jar" ]; then
  echo "ERROR: no existe $jar (ejecutar ./gradlew.bat bootJar)" >&2
  exit 1
fi

if [ -z "${ODONTO_IMAGEN_ANTERIOR:-}" ] || [ -z "${ODONTO_DUMP_ANTERIOR:-}" ]; then
  echo "ERROR: faltan ODONTO_IMAGEN_ANTERIOR u ODONTO_DUMP_ANTERIOR (pg_dump -Fc anterior)" >&2
  exit 1
fi

java -cp "$jar" ar.edu.utn.odonto.operacion.RollbackDespliegue "$slug"

echo "rollback completado; evento_auditoria conservado: cualquier correccion se registra como evento nuevo"
