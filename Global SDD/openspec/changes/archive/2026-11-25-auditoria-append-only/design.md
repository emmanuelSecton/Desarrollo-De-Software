# Design: auditoria-append-only (C-02)

## Context

Ver `proposal.md` para el motivo. Estado real verificado: C-01 archivado (spec `plataforma-base`), módulo `auditoria` con sólo `package-info.java`, `V1__baseline.sql` intacto (sólo extensiones), sin infraestructura de eventos. `deploy/postgres/init/01-base-consultorio.sh` crea el rol migrator como OWNER de la base y un rol runtime separado (`APP_DB_USER`, por defecto `app_runtime`); la app corre con el rol runtime y Flyway se ejecuta con el migrator en un servicio `migrate` de corta duración, compatible con DD-01: la garantía fuerte es el trigger append-only y la defensa en profundidad es la separación de roles.

Restricciones que condicionan el diseño:
- ArchUnit de C-01: en cada módulo `api → application → domain`, `infrastructure` implementa puertos; `application` NO puede depender de `infrastructure`; un módulo sólo usa `application` de otro módulo; sin ciclos; prohibido `java.time.*.now()` sin `Clock`. Los eventos van en `<modulo>.application.evento` para que sean API pública y no rompan módulos vecinos.
- `Clock` UTC inyectado en `shared/infrastructure/ConfiguracionTiempo.java`; sesión JDBC en UTC; `ddl-auto: validate`.
- Testcontainers real PostgreSQL 16 (`support/PostgresTestConfig.java`), nunca H2 ni mocks de base. JaCoCo ≥ 80 %.
- `MigracionBaselineTest` hoy espera exactamente `1:baseline` y una sola fila en `flyway_schema_history`: agregar V2 exige actualizar esa expectativa en Apply.

## Goals / Non-Goals

**Goals:**
- Garantizar, en base y por código, que los eventos de auditoría sólo se insertan y leen.
- Proveer un único punto de registro para escrituras transaccionales y lecturas sin transacción.
- Cumplir minimización de datos (Ley 25.326) en `detalle`, logs y eventos.
- Mantener compatibilidad con arranque local, tests de C-01 y spec `plataforma-base`.

**Non-Goals:** consulta HTTP paginada (C-32); detrás-de-proxy de IP (C-35); FK a `usuario`/`paciente` (C-03/C-09); hash encadenado y retención (C-35/historia clínica); Spring Modulith; UI.

## Decisions

### D1. Migración `V2__auditoria.sql` (DDL exacto)

Nueva migración; NUNCA editar `V1__baseline.sql`. No se usa enum de PostgreSQL para `accion` ni para `actor_tipo`: se persisten como `VARCHAR` con `CHECK`, de modo que altas futuras no requieran `ALTER TYPE`.

```sql
CREATE TABLE evento_auditoria (
    id           BIGSERIAL PRIMARY KEY,
    ocurrido_at  TIMESTAMPTZ NOT NULL,
    actor_tipo   VARCHAR(20) NOT NULL,
    actor_id     VARCHAR(100),
    accion       VARCHAR(60) NOT NULL,
    entidad      VARCHAR(80) NOT NULL,
    entidad_id   VARCHAR(100) NOT NULL,
    detalle      JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip           INET,
    CONSTRAINT ck_evento_auditoria_actor CHECK (
        (actor_tipo = 'SISTEMA' AND actor_id IS NULL)
        OR (actor_tipo IN ('USUARIO', 'PACIENTE') AND actor_id IS NOT NULL)
    ),
    CONSTRAINT ck_evento_auditoria_detalle_objeto CHECK (jsonb_typeof(detalle) = 'object')
);

CREATE OR REPLACE FUNCTION rechazar_modificacion_evento_auditoria() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'evento_auditoria es append-only: UPDATE/DELETE no permitidos'
        USING ERRCODE = '42501';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_evento_auditoria_append_only
    BEFORE UPDATE OR DELETE ON evento_auditoria
    FOR EACH ROW EXECUTE FUNCTION rechazar_modificacion_evento_auditoria();

CREATE OR REPLACE FUNCTION rechazar_truncate_evento_auditoria() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'evento_auditoria es append-only: TRUNCATE no permitido'
        USING ERRCODE = '42501';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_evento_auditoria_no_truncate
    BEFORE TRUNCATE ON evento_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION rechazar_truncate_evento_auditoria();

CREATE INDEX idx_evento_auditoria_ocurrido_at ON evento_auditoria (ocurrido_at DESC);
CREATE INDEX idx_evento_auditoria_actor ON evento_auditoria (actor_tipo, actor_id);
CREATE INDEX idx_evento_auditoria_accion ON evento_auditoria (accion);
CREATE INDEX idx_evento_auditoria_entidad ON evento_auditoria (entidad, entidad_id);

GRANT USAGE, SELECT ON SEQUENCE evento_auditoria_id_seq TO ${runtime_role};
GRANT SELECT, INSERT ON evento_auditoria TO ${runtime_role};
```

Notas: `ocurrido_at` NO tiene `DEFAULT now()` (debe venir del `Clock` inyectado, RN-GL-02); `detalle` usa `DEFAULT '{}'` sólo para eventos sin metadatos. `${runtime_role}` lo resuelve Flyway desde `APP_DB_USER` (por defecto `app_runtime`), sin rol runtime hardcodeado en la migración. `TRUNCATE` no dispara `BEFORE DELETE`, por eso se bloquea con un trigger `BEFORE TRUNCATE ... FOR EACH STATEMENT` que aplica incluso al dueño de la tabla. Sin FK a usuario/paciente: cuando esas tablas existan (C-03/C-09) se evaluará una FK **diferida o no** según convenga, sin acoplar hoy los módulos. Convención para `LOGIN`: `entidad = 'USUARIO'`, `entidad_id = actorId`. Alternativa descartada: enum de PostgreSQL (exigiría migración por cada acción nueva) o `DEFAULT now()` (rompe inyectabilidad del reloj).

### D2. Roles y configuración (dos capas, compatible con DD-01)

- **Roles de la instancia single-tenant**: rol migrator (`app_migrator`) y rol runtime (`APP_DB_USER`, por defecto `app_runtime`). Sólo contraseñas y nombre de base son configurables por entorno; C-35 parametrizará los nombres para múltiples consultorios. El nombre del runtime se inyecta como placeholder `${runtime_role}` de Flyway, resuelto desde `APP_DB_USER` para los grants de `V2__auditoria.sql`. Se adopta el placeholder tras la revisión del PR: evita un rol hardcodeado en la migración y exige coordinar init script + migración + tests.
- `deploy/postgres/init/01-base-consultorio.sh` pasa a: validar `APP_DB_NAME`, `APP_DB_MIGRATOR_USER`, `APP_DB_MIGRATOR_PASSWORD`, `APP_DB_USER`, `APP_DB_PASSWORD`; crear ambos roles `LOGIN`; `CREATE DATABASE :"db" OWNER :"migrator"`; `REVOKE CONNECT ... FROM PUBLIC`; `GRANT CONNECT ...` al rol runtime indicado por `APP_DB_USER`; conectarse a la base y `GRANT USAGE ON SCHEMA public` a ese mismo rol. El runtime queda sin `UPDATE`/`DELETE`/`TRUNCATE` y sin DDL; el migrator conserva DDL y es el único que ejecuta Flyway.
- `deploy/docker-compose.yml`: el servicio `app` recibe sólo `DB_URL`/`DB_USER`/`DB_PASSWORD` del rol runtime (`DB_USER=APP_DB_USER`) y tiene Flyway deshabilitado (`SPRING_FLYWAY_ENABLED=false`): no puede ejecutar migraciones. Un servicio `migrate` de corta duración usa `SPRING_FLYWAY_URL`, `SPRING_FLYWAY_USER` y `SPRING_FLYWAY_PASSWORD` desde `APP_DB_MIGRATOR_*` (relaxed binding), pasa `${runtime_role}` desde `APP_DB_USER`, aplica Flyway y termina. En el perfil de test no existe ese servicio: Flyway usa el `DataSource` inyectado por `@ServiceConnection`, igual que en C-01. `application.yml` mantiene `spring.datasource.*` con `DB_URL/DB_USER/DB_PASSWORD` y `spring.flyway.validate-on-migrate: true` para el flujo de migración; no se fijan credenciales Flyway vacías por defecto en `app` para no romper el test de arranque.
- `.env.example`: documenta `APP_DB_MIGRATOR_USER` / `APP_DB_MIGRATOR_PASSWORD` (secreto vacío), `APP_DB_USER` con valor por defecto `app_runtime` como rol runtime, y que `DB_USER`/`DB_PASSWORD` deben coincidir con él. Sin valores reales.
- **Testcontainers**: se mantiene `PostgresTestConfig` y su `@ServiceConnection` con el usuario del contenedor como migrator; se agrega un init script de test (`backend/src/test/resources/db/init/auditoria-roles.sql`) que crea el rol runtime `LOGIN PASSWORD '<ficticio>'` vía `PostgreSQLContainer.withInitScript(...)`. Los tests de `RepositorioAuditoriaJdbc` usan un `DataSource` con ese rol no superusuario; ningún test existente cambia de conexión: los tests de permisos abren una conexión JDBC separada con ese rol sobre la misma URL del contenedor. Así `ArranqueTest` y el arranque local siguen funcionando.
- `MigracionBaselineTest` se actualiza en Apply para esperar `["1:baseline", "2:auditoria"]` y dos filas de historial; es un ajuste de expectativa, no una regresión.
- Orden de arranque: init script → servicio `migrate` (Flyway con el rol migrator y `${runtime_role}` resuelto) → `app` (rol runtime, sin Flyway). Rollback: C-02 no se despliega solo sobre una base con datos previos en la demo; si se necesita, `DROP TABLE evento_auditoria` la hace el migrator y se revierten grants (no hay `DOWN` migration, coherente con Flyway).

### D3. Contrato Java del módulo (API pública = `application`)

```java
public record RegistroAuditoria(
        AccionAuditoria accion,
        TipoActor tipoActor,
        String actorId,
        String entidad,
        String entidadId,
        Map<String, Object> detalle,
        String ip) {}

public interface AuditoriaService {
    void registrar(RegistroAuditoria registro);
}
```

- `AccionAuditoria` en `auditoria.application`: extensible, con al menos `LOGIN`, `LOGIN_FALLIDO`, `VER_HISTORIA`, `CREAR_TURNO`, `EXPORTAR`, `CREAR_ENTIDAD`, `MODIFICAR_ENTIDAD`, `ANULAR_ENTIDAD`; cada valor lleva el texto persistido (`VARCHAR(60)`). No es un enum de base.
- `TipoActor` en `auditoria.application`: `SISTEMA`, `PACIENTE`, `USUARIO`; `actorId` e `ip` nullable; C-02 no valida pertenencia a tablas de usuario/paciente.
- `EventoAuditoria` (record) en `auditoria.application.evento`, con los mismos campos; lo publican otros módulos con `ApplicationEventPublisher`. Auditoría no importa clases internas de otros módulos.
- `AuditoriaServiceImpl.registrar` es `@Transactional(propagation = Propagation.REQUIRED)`: participa de la transacción de negocio activa o abre una nueva si no hay ninguna. Recibe un `RegistroAuditoria`, lo valida antes de registrar y delega en un puerto de salida definido en `application` (p. ej. `EventoAuditoriaPort`), implementado en `auditoria.infrastructure`. Así se respeta la regla ArchUnit que prohíbe `application → infrastructure`.
- Persistencia en `infrastructure` con JDBC/`NamedParameterJdbcTemplate`: `ocurrido_at` se obtiene del `Clock` inyectado (`clock.instant()`), `ip` se convierte a `PGobject` de tipo `inet`, `detalle` a JSONB. No se expone entidad JPA ni endpoint en C-02. Alternativa descartada: JPA con entidad mapping de INET (dependencia de dialecto y riesgo de exponerla en API).

### D4. Listener, BEFORE_COMMIT y fallos

- `AuditoriaListener` en `auditoria.infrastructure` (necesita el puerto), con `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)`, delega en `AuditoriaServiceImpl.registrar`. La anotación se aplica en el listener (bean distinto del servicio) para que el `@Transactional(REQUIRED)` del servicio sea efectivo por proxy. El evento se escribe en la misma transacción de negocio: no hay registro posterior a un commit ya confirmado.
- `fallbackExecution = true` cubre lecturas sin transacción activa como `VER_HISTORIA`: el listener se ejecuta igual y el servicio, con `REQUIRED`, abre una transacción nueva y fresca para el evento. Este es el contrato que C-23 invocará (RN-SEG-05).
- Si falla el registro, se emite el log estructurado y la excepción se propaga: la transacción de negocio se revierte, sin confirmar la operación sin su huella (RN-GL-01 / Ley 26.529). El log usa MDC únicamente con `auditoria.accion`, `auditoria.entidad`, `auditoria.motivo` y un id de correlación aleatorio, más mensaje fijo y excepción técnica; NUNCA `auditoria.entidad_id`, `entidad_id`, identificadores de paciente, valores de `detalle`, IP ni datos clínicos. Alternativa descartada: `AFTER_COMMIT` con excepción capturada y no propagada (dejaría la operación confirmada sin auditoría, violando RN-GL-01).

### D5. `detalle` e `ip`

- Whitelist estricta de claves de referencia: `turno_id`, `paciente_id`, `usuario_id`, `historia_id`, `entrada_id`, `cobro_id`, `referencia`, `motivo_codigo`. Cualquier clave fuera de la whitelist se rechaza, sin loguear su valor. El valor de `referencia` MUST cumplir `^[A-Za-z0-9_-]{1,64}$`; una referencia vacía, demasiado larga o con otros caracteres se rechaza sin persistir ni repetir el valor en logs o mensajes de error.
- Denylist adicional por patrones (`diagnostico`, `email`, `telefono`, `dni`, `nombre`, `apellido`, `direccion`, `password`, `token`, `clave`, `nota`, `observacion`) como defensa ante futuras ampliaciones de la whitelist. Valores: sólo escalares (String/Number/Boolean), longitud máxima 200 caracteres, sin objetos ni arrays anidados.
- Validación en `application` antes de publicar/registrar: `accion`, `tipoActor`, `entidad` y `entidadId` nulos o con longitud superior a 60/20/80/100 respectivamente lanzan `AuditoriaInvalidaException`, sin incluir nunca el valor rechazado en el mensaje ni en logs. `ProblemDetail` sólo si algún día hay endpoint (C-32); en C-02 se traduce a excepción de dominio y al log seguro.
- `ip`: nullable; si presente, se valida de forma pura y sintáctica como IPv4/IPv6, sin resolver hostnames ni consultar DNS, y se persiste como `INET`. La IP detrás de proxy se difiere a C-35.

### D6. Impacto downstream y tests

- Contrato estable para C-03 (`LOGIN`, `LOGIN_FALLIDO`), C-09/C-06 (altas/modificaciones), C-10 (`CREAR_TURNO`), C-14/C-16 (`PACIENTE`), C-23 (`VER_HISTORIA`), C-25/C-33/C-36 (`EXPORTAR`) y C-32 (índices ya listos para filtros `actor/accion/entidad/desde/hasta`).
- Estrategia de tests (Apply, TDD estricto): safety net inicial con la suite C-01; RED → GREEN → TRIANGULAR (mínimo caso feliz + edge) → REFACTOR por tarea; JUnit 5 + AssertJ + Testcontainers PostgreSQL 16; `RepositorioAuditoriaJdbc` se ejecuta con un `DataSource` del rol runtime no superusuario, nunca H2 ni mocks de base; trigger y permisos con conexiones reales, service/listener con `Clock` fijo y logger capturado; arquitectura con `ArquitecturaTest`; cobertura JaCoCo ≥ 80 %.

## Risks / Trade-offs

- [El servicio `app` ejecuta Flyway con privilegios indebidos] → `app` corre sin Flyway y sólo con credenciales `DB_*` del runtime; el servicio `migrate` efímero aplica migraciones con el rol migrator y `${runtime_role}` desde `APP_DB_USER`; test de que el runtime no puede `UPDATE`/`DELETE`.
- [El trigger no evitaba que el dueño hiciera `TRUNCATE`] → `TRUNCATE` no dispara `BEFORE DELETE`, pero el trigger `BEFORE TRUNCATE ... FOR EACH STATEMENT` rechaza la sentencia incluso para el dueño de la tabla; se cubre con un test de conexión real.
- [Cambio de roles rompe el despliegue actual si no se recrea el volumen] → el init script corre sólo al crear el volumen; `.env.example` y el runbook indican recrear el volumen en la demo (datos ficticios, SU-07) o ejecutar el script de elevación única antes de Apply.
- [MigracionBaselineTest falla por la nueva migración] → actualizar su expectativa en la misma tarea TDD; no es regresión funcional.
- [Logs estructurados sin librería nueva] → MDC con `auditoria.accion`, `auditoria.entidad`, `auditoria.motivo` e id de correlación aleatorio, más mensaje fijo; nunca `entidad_id` ni identificadores de paciente. Si más adelante se agrega encoder JSON, los campos ya están separados. Alternativa considerada: `logstash-logback-encoder` (dependencia nueva evitable).
- [Coherencia entre operación y evento] → el registro ocurre en la misma transacción (`BEFORE_COMMIT`, `REQUIRED`); si falla, se revierte la operación y se emite un log seguro. Ya no se tolera perder el evento de una operación confirmada (RN-GL-01 / Ley 26.529).

## Migration Plan

1. Actualizar init script, compose (`app` sin Flyway y servicio `migrate`) y `.env.example`; recrear volumen de la demo o crear los roles en la base existente con el migrator.
2. Ejecutar el servicio `migrate` al desplegar: Flyway aplica `V2__auditoria.sql` con el rol migrator, resuelve `${runtime_role}` desde `APP_DB_USER` y valida el historial; después el servicio termina.
3. Verificar con Testcontainers: trigger de `UPDATE`/`DELETE`/`TRUNCATE`, grants con el rol runtime no superusuario, CHECK, índices; actualizar `MigracionBaselineTest`.
4. Los módulos posteriores publican eventos; el runtime de la app nunca ejecuta Flyway y registra la auditoría en la misma transacción de negocio.

## Open Questions

Ninguna pendiente que cambie specs, enfoque o tareas. Diferidos explícitos por decisión del usuario: resolución de IP detrás de proxy (C-35), consulta paginada (C-32) y FK a usuario/paciente (posterior a C-03/C-09).
