# Tasks: auditoria-append-only (C-02)

Convención de Apply: TDD estricto. Cada tarea sigue RED (test que falla) → GREEN (mínimo código) → TRIANGULAR (mínimo 2 casos: feliz + borde) → REFACTOR (tests en verde tras cada paso). Sin código de producción ni tests ejecutados durante Propose. Las rutas de test son propuestas; el agente de Apply puede ajustar el nombre del archivo, no la cobertura exigida.

## 1. Safety net y separación de roles

- [x] 1.1 Capturar safety net: ejecutar `ArranqueTest`, `ArquitecturaTest` (incluye `ReglasArquitecturaFixtureTest`) y `MigracionBaselineTest` sobre el estado actual y registrar "N/N tests verdes"; si alguno falla, STOP y reportar fallo preexistente sin arreglarlo. Verificación:baseline documentado en el resumen de Apply antes de cualquier cambio.
- [x] 1.2 Actualizar `deploy/postgres/init/01-base-consultorio.sh` para crear `app_migrator` (owner de la base, DDL/Flyway) y `app_runtime` (login, CONNECT y USAGE de schema, sin DDL); documentar `APP_DB_MIGRATOR_USER`/`APP_DB_MIGRATOR_PASSWORD` y el rol runtime en `.env.example`. Verificación: `RolesInicialesTest` con PostgreSQL real valida que la base es propiedad del migrator y que el runtime existe sin privilegios de DDL; `.env.example` sin secretos reales. RED→GREEN→TRIANGULAR (migrator/runtime)→REFACTOR.
- [x] 1.3 Configurar `deploy/docker-compose.yml` para pasar `DB_USER/DB_PASSWORD` del runtime y `SPRING_FLYWAY_URL/USER/PASSWORD` del migrator, dejando el perfil de test sin credenciales Flyway explícitas para que `@ServiceConnection` siga funcionando. Verificación: `ArranqueTest` y `MigracionBaselineTest` siguen verdes y una comprobación de configuración demuestra que Flyway usa el migrator en compose. RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 1.4 Agregar el init script de Testcontainers (`backend/src/test/resources/db/init/auditoria-roles.sql`) y enlazarlo desde `PostgresTestConfig` sin cambiar el `@ServiceConnection` existente. Verificación: `ConexionRuntimeTest` abre una segunda conexión con `app_runtime` sobre la misma URL del contenedor; `ArranqueTest` continúa verde. RED→GREEN→TRIANGULAR→REFACTOR.

## 2. Migración y garantías de base

- [x] 2.1 Crear `V2__auditoria.sql` con la tabla, columnas exactas, `accion VARCHAR(60)`, `detalle JSONB`, `ip INET` nullable, `ocurrido_at TIMESTAMPTZ NOT NULL` sin `DEFAULT now()`, los cuatro índices exactos y los GRANT a `app_runtime`; no tocar `V1__baseline.sql`. Verificación: `EsquemaAuditoriaTest` sobre PostgreSQL 16 real comprueba tipos, índices `(ocurrido_at DESC)`, `(actor_tipo, actor_id)`, `(accion)`, `(entidad, entidad_id)` y ausencia de default temporal. RED→GREEN→TRIANGULAR (columnas + índices)→REFACTOR.
- [x] 2.2 Implementar el trigger `BEFORE UPDATE OR DELETE` que rechaza modificaciones y borrados. Verificación: `TriggerAppendOnlyTest` con dos casos — UPDATE de un campo y DELETE de la fila — ambos fallan y la fila permanece idéntica, ejecutado con Testcontainers (nunca H2/mocks). RED→GREEN→TRIANGULAR (UPDATE/DELETE + verificación de fila)→REFACTOR.
- [x] 2.3 Implementar el CHECK condicional de actor en la base. Verificación: `CheckActorTest` cubre SISTEMA sin id, USUARIO con id y PACIENTE con id (casos válidos) y más los dos inválidos (SISTEMA con id, USUARIO/PACIENTE sin id). RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 2.4 Verificar los permisos del rol runtime con conexión real. Verificación: `PermisosRuntimeTest` demuestra SELECT y INSERT (incluido `USAGE` de la secuencia `evento_auditoria_id_seq`) y deniega UPDATE y DELETE; además comprueba que el runtime no ejecuta Flyway y que el migrator conserva DDL. RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 2.5 Actualizar `MigracionBaselineTest` para esperar `["1:baseline", "2:auditoria"]` y dos filas de historial, conservando sus demás aserciones. Verificación: el test queda verde y el segundo arranque no reaplica migraciones. RED→GREEN→TRIANGULAR→REFACTOR.

## 3. Contrato público y persistencia del módulo

- [x] 3.1 Definir `RegistroAuditoria`, `AccionAuditoria` (extensible: `LOGIN`, `LOGIN_FALLIDO`, `VER_HISTORIA`, `CREAR_TURNO`, `EXPORTAR`, altas/modificaciones/anulaciones genéricas), `TipoActor` y el record de evento en `auditoria.application.evento`, sin enum de PostgreSQL. Verificación: `ContratoAuditoriaTest` comprueba los valores persistidos como `VARCHAR(60)` y que agregar una acción no altera la base. RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 3.2 Implementar la validación de `detalle` (whitelist de claves de referencia, denylist de patrones sensibles, valores escalares y longitud máxima) y de `ip` (nullable, IPv4/IPv6 válidos, sin hostnames). Verificación: `ValidadorDetalleTest` y `ValidadorIpTest` cubren caso permitido, clave sensible rechazada sin loguear el valor, IP nula, IP válida e IP inválida. RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 3.3 Implementar `AuditoriaService.registrar(RegistroAuditoria)` con `@Transactional(REQUIRES_NEW)`, delegando en un puerto de `application`, y su adaptador JDBC en `infrastructure` que toma `ocurrido_at` del `Clock` inyectado y convierte `ip`/`detalle` a tipos PostgreSQL. Verificación: `AuditoriaServiceTest` con reloj fijo en `2026-11-24T13:00:00Z` y `RepositorioAuditoriaTest` con Testcontainers validan fila persistida, igualdad de `ocurrido_at` y transacción independiente. RED→GREEN→TRIANGULAR→REFACTOR.

## 4. Listener post-commit, fallos y lecturas

- [x] 4.1 Implementar el listener en `infrastructure` con `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` que delega en el servicio. Verificación: `AuditoriaListenerTest` confirma que el evento se persiste sólo después del commit de la operación de negocio y que ambas transacciones terminan correctamente. RED→GREEN→TRIANGULAR (commit + rollback sin auditoría)→REFACTOR.
- [x] 4.2 Implementar el manejo de fallo post-commit con log estructurado vía MDC (acción, entidad, entidad_id, motivo) sin lanzar la excepción hacia la operación confirmada. Verificación: `AuditoriaListenerFalloTest` captura el log y afirma que contiene los metadatos técnicos, que no contiene datos clínicos/PII y que la operación de negocio permanece confirmada. RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 4.3 Cubrir el contrato de lecturas sin transacción (`VER_HISTORIA`, RN-SEG-05) mediante `fallbackExecution`. Verificación: `AuditoriaSinTransaccionTest` publica el evento sin transacción activa y comprueba registro equivalente al de una escritura transaccional. RED→GREEN→TRIANGULAR→REFACTOR.

## 5. Verificación integral

- [x] 5.1 Ejecutar la suite completa de no regresión: `ArranqueTest`, `ArquitecturaTest`, `ReglasArquitecturaFixtureTest` y `MigracionBaselineTest`, además de los tests nuevos; verificar que ArchUnit no detecta dependencias de `auditoria.application` a `infrastructure`, ni módulos internos ajenos, ni `now()` directo. Verificación: suite verde y reglas de `ReglasArquitectura` aplicadas a los eventos en `application.evento`.
- [x] 5.2 Verificar cobertura y calidad: `jacocoTestCoverageVerification` con mínimo 0.80 de líneas y `check` completo. Verificación: build verde sin tests en rojo y sin secretos en `.env.example` ni en el log de fallos.

## Workflow follow-up

- Revisión humana de los artefactos y del código antes de Apply (governance CRITICO).
- Marcar C-02 como completado en `CHANGES.md` y archivar el change con OpenSpec recién después de que el usuario lo autorice.
