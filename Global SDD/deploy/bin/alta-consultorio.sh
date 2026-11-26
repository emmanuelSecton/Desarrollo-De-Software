#!/bin/bash
set -euo pipefail

if [ "$#" -lt 3 ]; then
  echo "uso: $0 <slug> <dominio> <directorio-env>" >&2
  exit 1
fi

slug="$1"
dominio="$2"
directorio_env="$3"
jar="${ODONTO_JAR:-../backend/build/libs/app.jar}"

if [ ! -f "$jar" ]; then
  echo "ERROR: no existe $jar (ejecutar ./gradlew.bat bootJar)" >&2
  exit 1
fi

export ODONTO_APPLY=1
java -cp "$jar" ar.edu.utn.odonto.operacion.AltaConsultorio \
  "$slug" "$dominio" "$directorio_env"
