# Design: auditoria-append-only (C-02)

## Context

Ver `proposal.md` para el motivo. Estado real verificado: C-01 archivado (spec `plataforma-base`), módulo `auditoria` con sólo `package-info.java`, `V1__baseline.sql` intacto (sólo extensiones), sin infraestructura de eventos. `deploy/postgres/init/01-base-consultorio.sh` crea `APP_DB_USER` como OWNER de la base y la app corre con ese mismo usuario; por eso un `REVOKE` sobre ese rol no protege nada: la garantía fuerte es el trigger y la defensa en profundidad es la separación de roles.

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

CREATE INDEX idx_evento_auditoria_ocurrido_at ON evento_auditoria (ocurrido_at DESC);
CREATE INDEX idx_evento_auditoria_actor ON evento_auditoria (actor_tipo, actor_id);
CREATE INDEX idx_evento_auditoria_accion ON evento_auditoria (accion);
CREATE INDEX idx_evento_auditoria_entidad ON evento_auditoria (entidad, entidad_id);

GRANT USAGE, SELECT ON SEQUENCE evento_auditoria_id_seq TO app_runtime;
GRANT SELECT, INSERT ON evento_auditoria TO app_runtime;
```

Notas: `ocurrido_at` NO tiene `DEFAULT now()` (debe venir del `Clock` inyectado, RN-GL-02); `detalle` usa `DEFAULT '{}'` sólo para eventos sin metadatos. Sin FK a usuario/paciente: cuando esas tablas existan (C-03/C-09) se evaluará una FK **diferida o no** según convenga, sin acoplar hoy los módulos. Convención para `LOGIN`: `entidad = 'USUARIO'`, `entidad_id = actorId`. Alternativa descartada: enum de PostgreSQL (exigiría migración por cada acción nueva) o `DEFAULT now()` (rompe inyectabilidad del reloj).

### D2. Roles y configuración (dos capas, compatible con C-01)

- **Nombres fijos de rol en C-02** (instancia single-tenant): `app_migrator` y `app_runtime`. Sólo contraseñas y nombre de base son configurables por entorno; C-35 parametrizará los nombres para múltiples consultorios. Alternativa descartada: inyectar el nombre del rol como placeholder de Flyway; requiere coordinar init script + migración + Tests y abre riesgo de interpolación de identificadores.
- `deploy/postgres/init/01-base-consultorio.sh` pasa a: validar `APP_DB_NAME`, `APP_DB_MIGRATOR_USER`, `APP_DB_MIGRATOR_PASSWORD`, `APP_DB_USER`, `APP_DB_PASSWORD`; crear ambos roles `LOGIN`; `CREATE DATABASE :"db" OWNER :"migrator"`; `REVOKE CONNECT ... FROM PUBLIC`; `GRANT CONNECT ... TO app_runtime`; conectarse a la base y `GRANT USAGE ON SCHEMA public TO app_runtime`. `app_runtime` queda sin `UPDATE`/`DELETE` y sin DDL; el migrator conserva DDL y ejecuta Flyway.
- `deploy/docker-compose.yml`: las `DB_*` del servicio `app` siguen apuntando al rol runtime (`DB_USER=APP_DB_USER`); se agregan `SPRING_FLYWAY_URL`, `SPRING_FLYWAY_USER` y `SPRING_FLYWAY_PASSWORD` desde `APP_DB_MIGRATOR_*` (relaxed binding de Spring Boot). En el perfil de test esas variables no existen: Flyway usa el `DataSource` inyectado por `@ServiceConnection`, igual que en C-01. `application.yml` mantiene `spring.datasource.*` con `DB_URL/DB_USER/DB_PASSWORD` y `spring.flyway.validate-on-migrate: true`; no se fijan credenciales Flyway vacías por defecto para no romper el test de arranque.
- `.env.example`: documenta `APP_DB_MIGRATOR_USER` / `APP_DB_MIGRATOR_PASSWORD` (secreto vacío), aclara que `APP_DB_USER` es el rol runtime y que `DB_USER`/`DB_PASSWORD` deben coincidir con él. Sin valores reales.
- **Testcontainers**: se mantiene `PostgresTestConfig` y su `@ServiceConnection` con el usuario del contenedor como migrator; se agrega un init script de test (`backend/src/test/resources/db/init/auditoria-roles.sql`) que crea `app_runtime LOGIN PASSWORD '<ficticio>'` vía `PostgreSQLContainer.withInitScript(...)`. Ningún test existente cambia de conexión: los tests de permisos abren una conexión JDBC separada con ese rol sobre la misma URL del contenedor. Así `ArranqueTest` y el arranque local siguen funcionando.
- `MigracionBaselineTest` se actualiza en Apply para esperar `["1:baseline", "2:auditoria"]` y dos filas de historial; es un ajuste de expectativa, no una regresión.
- Orden de arranque: init script → Flyway (migrator) → app (runtime). Rollback: C-02 no se despliega solo sobre una base con datos previos en la demo; si se necesita, `DROP TABLE evento_auditoria` la hace el migrator y se revierten grants (no hay `DOWN` migration, coherente con Flyway).

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
- `AuditoriaService.registrar` es `@Transactional(propagation = Propagation.REQUIRES_NEW)` y recibe un `RegistroAuditoria` ya validado; delega en un puerto de salida definido en `application` (p. ej. `EventoAuditoriaPort`), implementado en `auditoria.infrastructure`. Así se respeta la regla ArchUnit que prohíbe `application → infrastructure`.
- Persistencia en `infrastructure` con JDBC/`NamedParameterJdbcTemplate`: `ocurrido_at` se obtiene del `Clock` inyectado (`clock.instant()`), `ip` se convierte a `PGobject` de tipo `inet`, `detalle` a JSONB. No se expone entidad JPA ni endpoint en C-02. Alternativa descartada: JPA con entidad mapping de INET (dependencia de dialecto y riesgo de exponerla en API).

### D4. Listener, AFTER_COMMIT y fallos

- `AuditoriaListener` en `auditoria.infrastructure` (necesita el puerto), con `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)`, delega en `AuditoriaService.registrar`. La anotación se aplica en el listener (bean distinto del servicio) para que el `@Transactional(REQUIRES_NEW)` del servicio sea efectivo por proxy.
- `fallbackExecution = true` cubre lecturas sin transacción activa: el listener se ejecuta igual y el servicio abre su propia transacción. Este es el contrato que C-23 invocará para `VER_HISTORIA` (RN-SEG-05).
- Si falla el registro post-commit, se captura la excepción en el listener, se emite log estructurado y NO se lanza hacia la operación de negocio ya confirmada. El log usa MDC con campos `auditoria.accion`, `auditoria.entidad`, `auditoria.entidad_id`, `auditoria.motivo` más mensaje fijo y excepción técnica; nunca valores de `detalle`, IP de paciente ni datos clínicos. Alternativa descartada: propagar la excepción (revertiría una operación ya confirmada o daría error engañoso al usuario).

### D5. `detalle` e `ip`

- Whitelist estricta de claves de referencia: `turno_id`, `paciente_id`, `usuario_id`, `historia_id`, `entrada_id`, `cobro_id`, `referencia`, `motivo_codigo`. Cualquier clave fuera de la whitelist se rechaza, sin loguear su valor.
- Denylist adicional por patrones (`diagnostico`, `email`, `telefono`, `dni`, `nombre`, `apellido`, `direccion`, `password`, `token`, `clave`, `nota`, `observacion`) como defensa ante futuras ampliaciones de la whitelist. Valores: sólo escalares (String/Number/Boolean), longitud máxima 200 caracteres, sin objetos ni arrays anidados.
- Validación en `application` antes de publicar/registrar, con `ProblemDetail` sólo si algún día hay endpoint (C-32); en C-02 se traduce a excepción de dominio y al log sin valor sensible.
- `ip`: nullable; si presente, se valida sintácticamente como IPv4/IPv6 (sin resolver hostnames) y se persiste como `INET`. La IP detrás de proxy se difiere a C-35.

### D6. Impacto downstream y tests

- Contrato estable para C-03 (`LOGIN`, `LOGIN_FALLIDO`), C-09/C-06 (altas/modificaciones), C-10 (`CREAR_TURNO`), C-14/C-16 (`PACIENTE`), C-23 (`VER_HISTORIA`), C-25/C-33/C-36 (`EXPORTAR`) y C-32 (índices ya listos para filtros `actor/accion/entidad/desde/hasta`).
- Estrategia de tests (Apply, TDD estricto): safety net inicial con la suite C-01; RED → GREEN → TRIANGULAR (mínimo caso feliz + edge) → REFACTOR por tarea; JUnit 5 + AssertJ + Testcontainers PostgreSQL 16; trigger y permisos con conexiones reales, service/listener con `Clock` fijo y logger capturado; arquitectura con `ArquitecturaTest`; cobertura JaCoCo ≥ 80 %.

## Risks / Trade-offs

- [El rol runtime sigue teniendo DDL si alguien lo usa para Flyway] → configuración explícita de `SPRING_FLYWAY_*` en compose y test de que el runtime no puede `UPDATE`/`DELETE`; el trigger protege aunque haya DDL.
- [El trigger no evita que el dueño haga `TRUNCATE`] → `TRUNCATE` no dispara `BEFORE DELETE`; el rol runtime no es dueño y no tiene ese privilegio; se documenta que sólo el migrator (operador humano) podría hacerlo y queda fuera del runtime de la app.
- [Cambio de roles rompe el despliegue actual si no se recrea el volumen] → el init script corre sólo al crear el volumen; `.env.example` y el runbook indican recrear el volumen en la demo (datos ficticios, SU-07) o ejecutar el script de elevación única antes de Apply.
- [MigracionBaselineTest falla por la nueva migración] → actualizar su expectativa en la misma tarea TDD; no es regresión funcional.
- [Logs estructurados sin librería nueva] → MDC + mensaje fijo; si más adelante se agrega encoder JSON, los campos ya están separados. Alternativa considerada: `logstash-logback-encoder` (dependencia nueva evitable).
- [Pérdida del evento post-commit] → trade-off aceptado por decisión del usuario: se loguea y no se revierte la operación; C-32 podrá exponer alertas operativas en el futuro.

## Migration Plan

1. Actualizar init script y `.env.example`; recrear volumen de la demo o crear los roles en la base existente con el migrator.
2. Aplicar `V2__auditoria.sql` con el rol migrator al arrancar (Flyway valida el historial).
3. Verificar con Testcontainers: trigger, grants, CHECK, índices; actualizar `MigracionBaselineTest`.
4. Los módulos posteriores publican eventos; el runtime de la app nunca ejecuta Flyway.

## Open Questions

Ninguna pendiente que cambie specs, enfoque o tareas. Diferidos explícitos por decisión del usuario: resolución de IP detrás de proxy (C-35), consulta paginada (C-32) y FK a usuario/paciente (posterior a C-03/C-09).
