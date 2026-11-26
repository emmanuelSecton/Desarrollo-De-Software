# Runbook de operación — despliegue por instancia

## Rollback de una versión

1. Identificar el tag o digest de imagen estable anterior (`ODONTO_IMAGEN_ANTERIOR`). No usar `latest`.
2. Tomar un `pg_dump -Fc` de la base afectada antes de cualquier cambio y exportarlo como `ODONTO_DUMP_ANTERIOR`.
3. Ejecutar `deploy/bin/rollback.sh <slug>` con `ODONTO_IMAGEN_ANTERIOR` y `ODONTO_DUMP_ANTERIOR` exportados; el script guarda el estado previo, restaura con `--clean` y reafirma los grants del runtime.
4. Verificar `/actuator/health` del consultorio y revisar los eventos nuevos de auditoría.

Si el problema es de datos, restaurar el `pg_dump` previo en una base nueva y migrar con el script de base correspondiente. El historial de `evento_auditoria` es append-only: una corrección se registra como evento nuevo, nunca se reescribe. No existe comando de undo en Flyway Community; la estrategia es volver al tag/digest anterior más el backup.

## Verificaciones relacionadas

- `deploy/bin/verificar-aislamiento.sh`
- `deploy/bin/verificar-limites.sh`
- `deploy/bin/verificar-historial.sh`

## Backups y restauración por consultorio

- Ejecutar `deploy/bin/backup-diario.sh` con `CONSULTORIO_SLUGS` y `ODONTO_BACKUP_DIR` exportados. Genera un `pg_dump` por base y conserva 30 días; si falla, termina con error y dispara alerta.
- Para recuperar una sola base, ejecutar `deploy/bin/restaurar-consultorio.sh <slug> <archivo.dump>`. El script restaura únicamente `consultorio_<slug>`, reafirma `REVOKE CONNECT FROM PUBLIC` y `GRANT CONNECT` del runtime.
- Después de restaurar verificar roles y grants del runtime (CONNECT propio, sin DDL, sin CONNECT cruzado) y `/actuator/health`.
- Hacer una prueba trimestral de restore en un entorno de demo y registrar el resultado; nunca usar datos reales.

La demo usa solo datos ficticios (SU-07). Quedan fuera de alcance la migración a PostgreSQL gestionado (PA-13) y el proveedor de SMTP definitivo (PA-09).
