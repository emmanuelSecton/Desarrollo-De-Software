#!/bin/bash
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$raiz"

sha256sum -c deploy/migraciones.sha256

patron="flyway"" undo"
if grep -R "$patron" deploy/bin deploy/RUNBOOK-operacion.md 2>/dev/null; then
  echo "ERROR: el comando de undo no esta disponible en Flyway Community" >&2
  exit 1
fi

echo "historial verificado: V1__baseline.sql y V2__auditoria.sql intactas"
