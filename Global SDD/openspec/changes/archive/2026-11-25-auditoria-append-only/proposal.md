# Proposal: auditoria-append-only (C-02)

## Why

Toda acción que crea, modifica o anula datos de turnos, pacientes, historia clínica o caja debe generar una huella inalterable (RN-GL-01) y toda lectura sensible debe poder rastrearse (RN-SEG-05), con trazabilidad exigible por las Leyes 25.326 y 26.529. Hoy el repositorio no tiene tabla de auditoría, roles diferenciados ni eventos de dominio: C-01 entregó sólo la plataforma base. C-02 debe existir antes de C-03 (login), C-09 (pacientes) y C-10 (turnos), que ya emiten eventos que necesitan un destino único y seguro.

## What Changes

- Nueva migración `V2__auditoria.sql` con la tabla `evento_auditoria` (`BIGSERIAL`, `ocurrido_at TIMESTAMPTZ`, `actor_tipo`, `actor_id` nullable, `accion VARCHAR(60)`, `entidad`, `entidad_id`, `detalle JSONB`, `ip INET` nullable), CHECK condicional de actor, índice de unicidad accidental no requerido y los cuatro índices de consulta acordados.
- **Append-only en tres capas**: trigger `BEFORE UPDATE OR DELETE` que rechaza toda modificación o borrado y trigger `BEFORE TRUNCATE ... FOR EACH STATEMENT` que bloquea el TRUNCATE incluso para el dueño de la tabla (garantías fuertes, verificadas con PostgreSQL real) **y** separación de roles: el migrator dueño de la base ejecuta Flyway/DDL en un servicio `migrate` de corta duración; el runtime de la aplicación tiene sólo `SELECT` e `INSERT` (más `USAGE` de secuencia) sobre la tabla, sin `UPDATE`/`DELETE` (defensa en profundidad). El trigger gana porque hoy el rol de la app es OWNER y un `REVOKE` a ese mismo rol sería ineficaz.
- Módulo `auditoria` con API pública en `auditoria.application`: `RegistroAuditoria`, `AccionAuditoria` (enum extensible), `TipoActor` y `AuditoriaService.registrar(...)`; persistencia en `infrastructure` detrás de un puerto, sin exponer entidades JPA.
- Listener in-process `@TransactionalEventListener(phase = BEFORE_COMMIT, fallbackExecution = true)` que delega en `AuditoriaServiceImpl` con `@Transactional(REQUIRED)`: la auditoría se escribe en la misma transacción de la operación de negocio. Sin transacción activa (lecturas `VER_HISTORIA` de C-23), el servicio abre una transacción fresca. No hay registro posterior a un commit ya confirmado.
- Fallo de auditoría en escritura transaccional: la excepción se propaga y revierte la transacción de negocio (RN-GL-01 / Ley 26.529), sin confirmar la operación sin su huella. Log estructurado con MDC únicamente `auditoria.accion`, `auditoria.entidad`, `auditoria.motivo` e id de correlación aleatorio; NUNCA `entidad_id`, identificadores de paciente ni datos clínicos (la columna de base `entidad_id` se mantiene; sólo se excluye del log). La alternativa `AFTER_COMMIT` con excepción capturada queda descartada.
- `detalle JSONB` con whitelist de claves, `referencia` con regex `^[A-Za-z0-9_-]{1,64}$`, rechazo de campos nulos o demasiado largos y denylist de patrones sensibles; `ip INET` nullable y validada con parser puro IPv4/IPv6 (sin DNS ni resolución detrás de proxy en este change).
- `ocurrido_at` obtenido siempre del `Clock` UTC inyectado por la plataforma; prohibido `Instant.now()`/`DEFAULT now()`.
- Configuración y despliegue: init scripts de PostgreSQL, `.env.example`, `application.yml` y `deploy/docker-compose.yml`: `app` corre sólo con credenciales `DB_*` del runtime y Flyway deshabilitado; un servicio `migrate` efímero ejecuta Flyway con el rol migrator y termina. `V2__auditoria.sql` usa el placeholder `${runtime_role}` de Flyway desde `APP_DB_USER` (por defecto `app_runtime`). Script de init para Testcontainers sin romper `PostgresTestConfig` ni el arranque local.
- Tests (Apply): Testcontainers con PostgreSQL 16 real para triggers (`UPDATE`/`DELETE`/`TRUNCATE`) y permisos, incluido test de integración con el rol runtime; unitarios para el service, el listener, el reloj, el `detalle`, la `ip` y el logging seguro; verificación de no regresión de `ArranqueTest`, `ArquitecturaTest` y `MigracionBaselineTest` (esta última debe actualizarse para esperar también `2:auditoria`).
- **BREAKING (configuración de despliegue, no de API)**: el arranque local pasa a necesitar un usuario migrator distinto del usuario runtime de la app, con un servicio `migrate` de corta duración que aplica las migraciones y sale; se documenta en `.env.example` y en el compose.

Decisiones CERRADAS por el usuario (no renegociables):

1. **Dos capas obligatorias (RN-GL-01)**: triggers `BEFORE UPDATE OR DELETE` y `BEFORE TRUNCATE` + roles migrator/runtime con mínimo privilegio. El runtime nunca ejecuta Flyway; el migrator conserva DDL y lo ejecuta en el servicio `migrate`.
2. **BEFORE_COMMIT y fallos**: listener con `fallbackExecution = true` y servicio con `@Transactional(REQUIRED)`; el evento se escribe en la transacción de negocio y un fallo se propaga, revirtiendo la operación (RN-GL-01 / Ley 26.529), con log seguro sin `entidad_id` ni PII.
3. **Lecturas tipo `VER_HISTORIA`**: mismo contrato de registro para eventos transaccionales y para lecturas sin transacción activa, gracias a `fallbackExecution` y `REQUIRED` (transacción fresca para esos eventos).
4. **`ip`, `detalle` y contratos downstream**: `ip` nullable/validada (proxy → C-35); `detalle` sólo metadatos mínimos con whitelist; firma `void registrar(RegistroAuditoria)` en `auditoria.application`; `AccionAuditoria` extensible persistida como `VARCHAR(60)`; `TipoActor` con CHECK condicional y sin FK aún; `ocurrido_at` del `Clock` UTC; índices exactos `(ocurrido_at DESC)`, `(actor_tipo, actor_id)`, `(accion)`, `(entidad, entidad_id)`.

## Capabilities

### New Capabilities

- `auditoria`: registro inalterable de eventos de auditoría (tabla append-only con triggers de UPDATE/DELETE/TRUNCATE, separación de roles, contrato de registro, listener en la misma transacción, actores, tiempo y minimización de datos).

### Modified Capabilities

- Ninguna. `plataforma-base` no cambia sus requirementes: C-02 los consume (Clock UTC, esquema por migraciones, límites entre módulos) sin redefinirlos.

## Impact

- **Dependencia**: C-01 (`2026-11-24-scaffolding-monorepo-y-base-tecnica`, 27/27, spec `plataforma-base`). Sin otros cambios activos.
- **Governance**: CRITICO (auditoría, seguridad y cumplimiento). Aprobación humana dada para escribir estos artefactos; Apply requiere revisión explícita.
- **Código/toques futuros (no ejecutados en Propose)**: `backend/src/main/resources/db/migration/V2__auditoria.sql`, módulo `backend/src/main/java/ar/edu/utn/odonto/auditoria/{api,application,domain,infrastructure}`, `application.yml`, `.env.example`, `deploy/postgres/init/01-base-consultorio.sh`, `deploy/docker-compose.yml` y su init de tests. No se edita `V1__baseline.sql`.
- **API HTTP**: ningún endpoint nuevo ni cambia el prefijo `/api/v1`; la consulta paginada es C-32.
- **Downstream**: C-03 (`LOGIN`, `LOGIN_FALLIDO`), C-09 y C-06 (altas/modificaciones), C-10 (`CREAR_TURNO`), C-14/C-16 (actor `PACIENTE`), C-23 (`VER_HISTORIA`, RN-SEG-05), C-25/C-33/C-36 (`EXPORTAR`), C-32 (filtros paginados).
- **Criterios de verificación**: UPDATE, DELETE y TRUNCATE sobre `evento_auditoria` fallan en PostgreSQL real; rol runtime carece de `UPDATE`/`DELETE` pero puede `SELECT`/`INSERT` y usa la secuencia, con test de integración del repositorio con ese rol; CHECK de actor acepta SISTEMA sin id y exige id para USUARIO/PACIENTE; `ocurrido_at` igual al `Clock` fijo; evento persistido en la misma transacción de negocio; fallo de auditoría propaga y revierte la operación, con log MDC seguro (`auditoria.accion`, `auditoria.entidad`, `auditoria.motivo` + correlación aleatorio) sin `entidad_id` ni PII; lectura sin transacción registrada en transacción fresca; `detalle` rechaza claves sensibles, `referencia` inválida y campos nulos/demasiado largos; `ip` validada con parser puro; `V2__auditoria.sql` resuelve `${runtime_role}` desde `APP_DB_USER` y bloquea TRUNCATE; suite C-01 en verde y cobertura JaCoCo ≥ 80 %.
- **Fuera de alcance**: endpoint de consulta de auditoría (C-32); resolución de IP detrás de proxy (C-35); FK a `usuario`/`paciente` (tablas de C-03/C-09 aún no existen); hash encadenado y retención operativa (pertenecen a historia clínica/C-35); UI.
