# Proposal: despliegue-por-instancia-y-operacion (C-35)

## Why

C-01 entregó la imagen única y el compose base, y C-02 separó los roles `migrator`/`runtime`, pero hoy no existe automatización para dar de alta consultorios, actualizar N instancias desde una misma imagen versionada, aislar sus bases, restaurar una sola de ellas ni operar la demo con observabilidad mínima. `CHANGES.md` ubica este change en la Fase 8, pero el roadmap lo planifica en GATE 0 / paso 2 y lo declara prerrequisito de C-36: sin este sustrato operativo no hay staging temprano ni despliegue productivo verificable (IN-01 resuelta, DD-01 revisada). El usuario cerró los bloqueantes del Explore y autorizó esta fase de planeamiento.

## What Changes

- **D2 (alcance)**: operar la demo sobre **un VPS económico con Docker Compose**: Caddy con TLS automático y subdominio por consultorio, uno o dos contenedores de la app (SPA embebida en el jar), PostgreSQL 16 en contenedor con volumen persistente y una base por consultorio, y `pg_dump` diario a almacenamiento externo montado (DD-11). Solo datos ficticios (SU-07). **PostgreSQL gestionado (PA-13), alta disponibilidad (PA-09) y el salto a producción quedan explícitamente fuera de alcance**; C-36 cubrirá la parte productiva.
- **D1**: el nombre canónico del change es `despliegue-por-instancia-y-operacion`; se crea la capability nueva `despliegue-y-operacion` sin reescribir `plataforma-base` ni `auditoria` ni los artefactos archivados (D9).
- **D7 / C5**: convención única en el clúster por instancia: base `consultorio_<slug>`, rol migrador `consultorio_<slug>_migrator` (owner de esa base) y rol runtime `consultorio_<slug>_runtime` (solo `CONNECT` a su base y privilegios mínimos, con `REVOKE CONNECT ... FROM PUBLIC`). Se reutiliza el placeholder `${runtime_role}` de `V2__auditoria.sql` sin tocar V1/V2. Se exige test de aislamiento: el runtime de una instancia no puede conectarse a la base de otra (R1).
- **D6 / C9**: asistente de alta por **CLI/script**, sin wizard web ni UI: crea base, roles, `env_file` separado por instancia, subdominio en Caddy, contenedor y arranque, dejando `ADMIN_INITIAL_EMAIL` listo como variable para C-03. No se adelanta lógica de C-03, C-05 ni C-15 (D5).
- **D8 / C8**: backup diario `pg_dump` por base con retención **≥ 30 días** en directorio externo montado o destino configurable por variable, más script/runbook de prueba de restauración que verifique **roles y grants** (R6). Se respeta SU-07/PA-09: ningún dato real en la demo.
- **D3 / C6**: el rollout usa **dos consultorios de demo** como canarios; rollback documentado por **vuelta a la imagen anterior (tag/digest) + restore del `pg_dump`**, **sin `flyway undo`** (no disponible en Community) y respetando que `evento_auditoria` es append-only: nunca se reescribe, se corrige con evento/entrada nueva (R4).
- **D9**: un servicio `migrate` por base, alineado con el de C-02, aplica Flyway antes del arranque de `app` (`SPRING_FLYWAY_ENABLED=false` en la app). Cada versión se despliega desde la **misma imagen versionada** y se migra instancia por instancia, primero la canaria (R3).
- **R2**: extender los límites de CPU/memoria y pool de conexiones más allá de `app` (que ya tiene `cpus`/`mem_limit` y `DB_POOL_MAX_SIZE=5`) a `postgres`, `migrate` y Caddy, para mitigar vecino ruidoso.
- **R9 / D5**: logs en formato **JSON** respetando la whitelist de MDC de C-02, sin PII ni datos clínicos. Alertas técnicas mínimas: salud de la app (`/actuator/health`), base caída y fallo del job de backup/migración (R10). La alerta de token de Meta vencido se **difiere a C-15/C-16** (C7).
- **D4 / R5**: secretos solo por `env_file` separado por instancia y variables de entorno; `.env.example` y el runbook documentan las variables sin valores reales. Ningún token, clave ni contraseña en el repositorio.
- **D10 / R7 / R11**: governance **ALTO** confirmado. Se incluye en el alcance el **ajuste mínimo necesario** en `../.github/workflows/global-sdd-ci.yml` (fuera del subtree `Global SDD/`) para producir tag semántico/digest de imagen **sin push ni secretos reales**. Este trabajo se describe para Apply; **no se edita el workflow durante Propose**.
- **C4**: para evitar la apariencia de contradicción entre Fase 8 y el GATE 0, el proposal deja explícito que la infraestructura se aprovisiona temprano (C-35 es prerrequisito de C-36) mientras las funcionalidades de negocio siguen su cronograma de fases. C1, C2, C3, C5 y C6 del Explore quedan resueltos por D5, D2, D9, D7 y D3 respectivamente; C8, C9 y C10 por D8, D6 y D9.
- **BREAKING (configuración de despliegue, no de API)**: el compose actual usa `consultorio`, `app_migrator` y `app_runtime`; la operación por instancia pasa a exigir `consultorio_<slug>` y sus dos roles con el sufijo del slug, `env_file` propio por instancia y Caddy como proxy. Ningún endpoint ni contrato HTTP cambia.

## Capabilities

### New Capabilities

- `despliegue-y-operacion`: contrato observable de la operación por instancia — alta aislada de consultorios, imagen versionada, rollout canario y rollback, migración por base, backups/restore con roles y grants, límites de recursos, logs JSON y alertas técnicas mínimas, con secretos fuera del repositorio.

### Modified Capabilities

- Ninguna. `plataforma-base` y `auditoria` se consumen sin cambiar sus requisitos: `/actuator/health`, `TIMESTAMPTZ` UTC, configuración por entorno, roles `migrator`/`runtime` y append-only ya están publicados. No se crea delta para ellos.

## Impact

- **Dependencias**: solo C-01 (`scaffolding-monorepo-y-base-tecnica`); hereda C-02 (`auditoria-append-only`) sin modificarlo. C-36 depende de C-35. C-03, C-05 y C-15 solo ven superficie preparada (`ADMIN_INITIAL_EMAIL`, env_file, alertas técnicas), sin lógica adelantada.
- **Archivos previstos en Apply (no tocados en Propose)**: `deploy/docker-compose.yml`, `deploy/postgres/init/01-base-consultorio.sh`, nuevos scripts/runbook bajo `deploy/`, `.env.example`, `README.md`, `../.github/workflows/global-sdd-ci.yml`. No se editan `V1__baseline.sql`, `V2__auditoria.sql`, specs publicadas ni artefactos archivados.
- **Governance**: ALTO (`CHANGES.md:811`). La autorización actual cubre solo Propose; Apply exige inspección y confirmación explícitas.
- **Criterios de verificación**: alta de un consultorio desde cero; un usuario de base no puede conectarse a la base de otro; migrar todas las instancias de una versión a otra; restaurar un solo consultorio desde su `pg_dump` verificando roles y grants; rollback sin `flyway undo` ni reescritura de `evento_auditoria`; logs JSON sin PII/datos clínicos; secretos ausentes del repositorio.
- **Fuera de alcance**: producción gestionada/HA, UI o wizard de alta, seed real de odontólogo (C-03), configuración de consultorio/prestaciones (C-05), webhook/token de Meta y sus alertas (C-15/C-16), y cualquier commit/push.
