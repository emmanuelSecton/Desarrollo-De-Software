# Design: despliegue-por-instancia-y-operacion (C-35)

## Context

Ver `proposal.md` para la motivación y el encuadre Fase 8 / GATE 0. Estado observable hoy: `deploy/Dockerfile` ya produce una imagen única con SPA embebida, runtime no root, `TZ=UTC`, HEALTHCHECK sobre `/actuator/health` y `MaxRAMPercentage=75`; `deploy/docker-compose.yml` tiene `app` + `postgres` + `migrate` (Flyway `11.7.2`, `depends_on: service_completed_successfully`, base sin puerto publicado y límites `cpus`/`mem_limit` solo en `app`); `deploy/postgres/init/01-base-consultorio.sh` ya hace `REVOKE CONNECT ... FROM PUBLIC`; `.env.example` documenta `DB_POOL_MAX_SIZE=5`, `ADMIN_INITIAL_EMAIL` y `WA_VERIFY_TOKEN`; `application.yml` usa entorno y deja el Flyway de producción deshabilitado en compose; `V2__auditoria.sql` usa `${runtime_role}`. El CI en `../.github/workflows/global-sdd-ci.yml` solo hace build local de imagen, sin tag semántico ni digest. No hay registry, skills DevOps ni `.atl/skill-registry.md`; las convenciones se derivan de los archivos existentes.

## Goals / Non-Goals

**Goals:**
- Operar dos consultorios de demo aislados sobre un VPS con Caddy, Compose y PostgreSQL en contenedor, con alta por CLI.
- Hacer reproducible el ciclo alta → verificar → actualizar (canaria) → migrar → rollback → backup → restore → alertar.
- Mantener el contrato de C-02 (migrate efímero, runtime sin DDL, `${runtime_role}`, auditoría append-only) sin tocar migraciones aplicadas ni specs publicadas.

**Non-Goals:**
- PostgreSQL gestionado, HA, multi-región y producción (PA-13, PA-09, D2/R8).
- UI, wizard de alta, lógica de C-03/C-05/C-15 y alerta de token de Meta (D5/D6, C7).
- Publicar imágenes, escribir secretos reales, commitear o pushear; el CI solo calcula tag/digest local (D10).

## Decisions

- **D1 / unicidad del cambio**: se crea `openspec/changes/despliegue-por-instancia-y-operacion/` y la capability `despliegue-y-operacion`. La documentación operativa nueva vive bajo `deploy/`; el código de negocio no cambia.
- **D2 / demo primero (DD-11)**: un único `docker-compose.yml` por VPS con Caddy TLS, `postgres`, un `migrate` por base y una `app` por consultorio; dos canarios de demo con datos ficticios (SU-07). C-36 retoma la producción; por eso la Fase 8 no bloquea la preparación temprana (C4).
- **D3 / rollback**: ante fallo se vuelve al tag/digest anterior de la imagen y se restaura el `pg_dump` del consultorio. `flyway undo` queda descartado (Community no lo incluye) y `evento_auditoria`/`entrada_historia` nunca se UPDATE/DELETE/TRUNCATE: las correcciones son eventos nuevos. La misma imagen inmutable garantiza que el rollback de código sea determinista (R4/R7).
- **D4 / secretos**: un `env_file` por instancia, fuera del repo, inyectado por Compose; `.env.example` y el runbook documentan variables con valores vacíos. Los secretos nunca entran en logs, alertas ni en el workflow (R5).
- **D5 / superficie preparada, sin negocio adelantado**: el script de alta escribe `ADMIN_INITIAL_EMAIL` en el `env_file` pero no hace seed; las alertas cubren `/actuator/health`, base caída y fallo de backup/migración. Meta se difiere a C-15/C-16.
- **D6 / CLI de alta**: un script idempotente por slug valida el nombre, crea base y roles con la convención `consultorio_<slug>_{migrator,runtime}`, genera el `env_file`, agrega el bloque de ruteo de Caddy y levanta el contenedor con la imagen fijada por tag/digest. Sin wizard web (R9/C9).
- **D7 / aislamiento (DD-01, R1)**: cada base con `REVOKE CONNECT FROM PUBLIC`; el runtime recibe solo `CONNECT` a su base, `USAGE` de esquema y grants mínimos, con `${runtime_role}` resuelto por el `migrate` de esa base desde `APP_DB_USER`. Test de integración con PostgreSQL real: el runtime A no puede conectar a la base B.
- **D8 / backups (C8/R6)**: job diario de `pg_dump` por base a un volumen externo montado o destino configurable por variable, con retención ≥ 30 días. El runbook de restore incluye verificación explícita de roles y grants y una prueba trimestral; los datos son ficticios y PA-09 sigue abierta para producción.
- **D9 / specs y migrate**: capability nueva, sin deltas en `plataforma-base` ni `auditoria`. Se mantiene un servicio `migrate` efímero por base, como en C-02, ejecutando V1/V2 y futuras migraciones con el rol migrador; la app corre con `SPRING_FLYWAY_ENABLED=false`. No se editan V1 ni V2 ni artefactos archivados (R3).
- **D10 / CI y governance ALTO (R7/R11)**: el job `imagen` del workflow externo pasa a etiquetar la imagen con un tag semántico derivado del cambio y a reportar su digest, sin push ni secretos. Es un ajuste mínimo descrito para Apply; se ejecuta con revisión humana previa. No se toca ahora.
- **R2 / vecino ruidoso**: mantener `DB_POOL_MAX_SIZE=5` y `cpus`/`mem_limit` en `app`, y extender límites a `postgres`, `migrate` y Caddy; el pool acotado evita agotar conexiones del host.
- **R9 / logs**: configurar el encoder JSON de la app conservando la whitelist de MDC de C-02 (`auditoria.accion`, `auditoria.entidad`, `auditoria.motivo` y correlación), sin `entidad_id`, PII ni datos clínicos; el proxy solo agrega metadatos técnicos de acceso.

## Risks / Trade-offs

- **[R1] Radio de impacto compartido** → aislamiento por base/credenciales, test de conexión cruzada y verificación de salud por instancia; a ~30 consultorios se reevalúa DD-01.
- **[R2] Vecino ruidoso en el VPS** → límites en los cuatro contenedores y pool ≤ 5; se acepta pérdida de rendimiento pico frente a vecinos.
- **[R3] N migraciones por versión** → un `migrate` efímero por base y detención del rollout ante el primer fallo; nunca Flyway en la app.
- **[R4] Rollback vs. append-only** → imagen anterior por tag/digest + restore del dump; no se permite reescribir eventos históricos.
- **[R5] Fuga de secretos** → `env_file` separado, `.env.example` vacío, sin secretos en logs/CI/alertas.
- **[R6] Restore incompleto** → la prueba de restauración verifica roles, grants y `REVOKE CONNECT FROM PUBLIC`, además de filas.
- **[R7] Imagen no reproducible** → fijar tag/digest en el alta y en el rollout; el CI publica el digest sin push.
- **[R8] Confusión entre demo y producción** → runbook y README marcan que PA-13/HA son prerrequisito antes de datos reales.
- **[R10] Poca observabilidad** → tres alertas técnicas mínimas; token de Meta se difiere.
- **[R11] Governance ALTO** → inspección humana de los artefactos antes de Apply y de cada cambio de compose/credenciales.

## Migration Plan

1. Confirmar la imagen por tag/digest y el inventario de consultorios de demo (dos canarios), sin modificar migraciones aplicadas.
2. Ajustar compose/init/Caddy y el `env_file` por instancia con la convención `consultorio_<slug>`; verificar `REVOKE`/grants y el test de conexión cruzada.
3. Ejecutar el `migrate` efímero por base; comprobar el historial de Flyway y el placeholder `${runtime_role}`; arrancar cada `app` solo tras el éxito.
4. Desplegar la canaria, verificar `/actuator/health` y logs JSON; solo entonces extender al resto de instancias de demo.
5. Backup diario y prueba de restore de un solo consultorio verificando roles y grants; documentar rollback por tag/digest + restore, sin `flyway undo`.
6. Ajuste mínimo del CI externo para tag semántico/digest sin push; revisión humana final antes de cerrar Apply.

## Open Questions

Ninguna bloqueante: D1-D10 y C1-C10 quedaron cerrados por el usuario. Quedan regulaciones diferidas explícitamente a otros changes: PA-09/PA-13 (producción, C-36) y alerta de token de Meta (C-15/C-16).
