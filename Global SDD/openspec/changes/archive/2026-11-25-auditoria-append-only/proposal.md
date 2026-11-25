# Proposal: auditoria-append-only (C-02)

## Why

Toda acción que crea, modifica o anula datos de turnos, pacientes, historia clínica o caja debe generar una huella inalterable (RN-GL-01) y toda lectura sensible debe poder rastrearse (RN-SEG-05), con trazabilidad exigible por las Leyes 25.326 y 26.529. Hoy el repositorio no tiene tabla de auditoría, roles diferenciados ni eventos de dominio: C-01 entregó sólo la plataforma base. C-02 debe existir antes de C-03 (login), C-09 (pacientes) y C-10 (turnos), que ya emiten eventos que necesitan un destino único y seguro.

## What Changes

- Nueva migración `V2__auditoria.sql` con la tabla `evento_auditoria` (`BIGSERIAL`, `ocurrido_at TIMESTAMPTZ`, `actor_tipo`, `actor_id` nullable, `accion VARCHAR(60)`, `entidad`, `entidad_id`, `detalle JSONB`, `ip INET` nullable), CHECK condicional de actor, índice de unicidad accidental no requerido y los cuatro índices de consulta acordados.
- **Append-only en dos capas**: trigger `BEFORE UPDATE OR DELETE` que rechaza toda modificación o borrado (garantía fuerte, verificada con PostgreSQL real) **y** separación de roles: `migrator` dueño de la base ejecuta Flyway/DDL; `runtime` de la aplicación tiene sólo `SELECT` e `INSERT` (más `USAGE` de secuencia) sobre la tabla, sin `UPDATE`/`DELETE` (defensa en profundidad). El trigger gana porque hoy el rol de la app es OWNER y un `REVOKE` a ese mismo rol sería ineficaz.
- Módulo `auditoria` con API pública en `auditoria.application`: `RegistroAuditoria`, `AccionAuditoria` (enum extensible), `TipoActor` y `AuditoriaService.registrar(...)`; persistencia en `infrastructure` detrás de un puerto, sin exponer entidades JPA.
- Listener in-process `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` con `@Transactional(REQUIRES_NEW)`: la auditoría se escribe en su propia transacción, después de confirmarse la operación de negocio. Cubre de forma uniforme escrituras transaccionales y lecturas sin transacción activa (contrato para `VER_HISTORIA` de C-23).
- Fallo post-commit: log estructurado con acción, entidad, id y motivo técnico; sin datos clínicos ni PII; la operación de negocio ya confirmada NO se revierte ni se ve afectada.
- `detalle JSONB` con política de whitelist de claves y rechazo de claves sensibles; `ip INET` nullable y validada cuando presente, sin resolución detrás de proxy en este change.
- `ocurrido_at` obtenido siempre del `Clock` UTC inyectado por la plataforma; prohibido `Instant.now()`/`DEFAULT now()`.
- Configuración y despliegue: init scripts de PostgreSQL, `.env.example`, `application.yml` y `deploy/docker-compose.yml` para separar credenciales migrator/runtime; script de init para Testcontainers sin romper `PostgresTestConfig` ni el arranque local.
- Tests (Apply): Testcontainers con PostgreSQL 16 real para trigger y permisos; unitarios para el service, el listener, el reloj, el `detalle` y el logging; verificación de no regresión de `ArranqueTest`, `ArquitecturaTest` y `MigracionBaselineTest` (esta última debe actualizarse para esperar también `2:auditoria`).
- **BREAKING (configuración de despliegue, no de API)**: el arranque local pasa a necesitar un usuario migrator distinto del usuario runtime de la app; se documenta en `.env.example` y en el compose.

Decisiones CERRADAS por el usuario (no renegociables):

1. **Dos capas obligatorias (RN-GL-01)**: trigger `BEFORE UPDATE OR DELETE` + roles migrator/runtime con mínimo privilegio. El runtime nunca ejecuta Flyway; el migrator conserva DDL.
2. **AFTER_COMMIT y fallos**: listener con `fallbackExecution = true` y `REQUIRES_NEW`; error post-commit logueado de forma estructurada y sin romper la operación ya confirmada.
3. **Lecturas tipo `VER_HISTORIA`**: mismo contrato de registro para eventos transaccionales y para lecturas sin transacción activa, gracias a `fallbackExecution`.
4. **`ip`, `detalle` y contratos downstream**: `ip` nullable/validada (proxy → C-35); `detalle` sólo metadatos mínimos con whitelist; firma `void registrar(RegistroAuditoria)` en `auditoria.application`; `AccionAuditoria` extensible persistida como `VARCHAR(60)`; `TipoActor` con CHECK condicional y sin FK aún; `ocurrido_at` del `Clock` UTC; índices exactos `(ocurrido_at DESC)`, `(actor_tipo, actor_id)`, `(accion)`, `(entidad, entidad_id)`.

## Capabilities

### New Capabilities

- `auditoria`: registro inalterable de eventos de auditoría (tabla append-only, separación de roles, contrato de registro, listener post-commit, actores, tiempo y minimización de datos).

### Modified Capabilities

- Ninguna. `plataforma-base` no cambia sus requirementes: C-02 los consume (Clock UTC, esquema por migraciones, límites entre módulos) sin redefinirlos.

## Impact

- **Dependencia**: C-01 (`2026-11-24-scaffolding-monorepo-y-base-tecnica`, 27/27, spec `plataforma-base`). Sin otros cambios activos.
- **Governance**: CRITICO (auditoría, seguridad y cumplimiento). Aprobación humana dada para escribir estos artefactos; Apply requiere revisión explícita.
- **Código/toques futuros (no ejecutados en Propose)**: `backend/src/main/resources/db/migration/V2__auditoria.sql`, módulo `backend/src/main/java/ar/edu/utn/odonto/auditoria/{api,application,domain,infrastructure}`, `application.yml`, `.env.example`, `deploy/postgres/init/01-base-consultorio.sh`, `deploy/docker-compose.yml` y su init de tests. No se edita `V1__baseline.sql`.
- **API HTTP**: ningún endpoint nuevo ni cambia el prefijo `/api/v1`; la consulta paginada es C-32.
- **Downstream**: C-03 (`LOGIN`, `LOGIN_FALLIDO`), C-09 y C-06 (altas/modificaciones), C-10 (`CREAR_TURNO`), C-14/C-16 (actor `PACIENTE`), C-23 (`VER_HISTORIA`, RN-SEG-05), C-25/C-33/C-36 (`EXPORTAR`), C-32 (filtros paginados).
- **Criterios de verificación**: UPDATE y DELETE sobre `evento_auditoria` fallan en PostgreSQL real; rol runtime carece de `UPDATE`/`DELETE` pero puede `SELECT`/`INSERT` y usa la secuencia; CHECK de actor acepta SISTEMA sin id y exige id para USUARIO/PACIENTE; `ocurrido_at` igual al `Clock` fijo; evento post-commit exitoso persistido; fallo post-commit logueado sin PII y sin afectar la operación; lectura sin transacción registrada; `detalle` rechaza claves sensibles; suite C-01 en verde y cobertura JaCoCo ≥ 80 %.
- **Fuera de alcance**: endpoint de consulta de auditoría (C-32); resolución de IP detrás de proxy (C-35); FK a `usuario`/`paciente` (tablas de C-03/C-09 aún no existen); hash encadenado y retención operativa (pertenecen a historia clínica/C-35); UI.
