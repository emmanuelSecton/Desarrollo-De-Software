# Tasks: despliegue-por-instancia-y-operacion (C-35)

Convención de Apply: TDD estricto por tarea (safety net → RED → GREEN → TRIANGULATE mínimo 2 casos → REFACTOR), con tests en verde antes de marcar `[x]`. Gobernanza ALTO: detenerse para inspección humana antes de tocar credenciales/compose y antes de cerrar el change. Las rutas de test son propuestas; Apply puede ajustar el nombre del archivo, no la cobertura exigida. Ninguna de estas tareas se ejecutó durante Propose.

## 1. Safety net e inventario

- [x] 1.1 Capturar el safety net: ejecutar `./gradlew check` (incluye `ArranqueTest`, `MigracionBaselineTest`, `ArquitecturaTest`), `npm test` y `docker compose -f deploy/docker-compose.yml config`, y registrar "N/N tests verdes"; si algo falla, STOP y reportar fallo preexistente sin arreglarlo. Verificación: baseline documentado en el resumen de Apply antes de cualquier edición.
- [x] 1.2 Inventariar la imagen actual por tag/digest y los dos consultorios de demo con sus slug, base, roles, subdominio y `env_file`. Verificación: tabla de inventario en el resumen de Apply y ninguna migración aplicada modificada.

## 2. Convención de base/roles y alta por CLI

- [x] 2.1 Extender `deploy/postgres/init/01-base-consultorio.sh` para crear `consultorio_<slug>`, `consultorio_<slug>_migrator` (owner) y `consultorio_<slug>_runtime` (CONNECT propio, `USAGE`, `REVOKE CONNECT FROM PUBLIC`), resolviendo `${runtime_role}` sin editar V1/V2 ni inicializaciones ya aplicadas. Verificación: `RolesPorInstanciaTest` con PostgreSQL 16 real (Testcontainers, nunca H2/mocks) valida nombres, owner, grants y ausencia de DDL/destructivos en el runtime; RED→GREEN→TRIANGULAR (dos slugs)→REFACTOR.
- [x] 2.2 Implementar el CLI de alta por slug sin UI: valida slug, crea base/roles, genera `env_file` separado con `ADMIN_INITIAL_EMAIL` listo para C-03, registra el subdominio y levanta el contenedor con la imagen fijada; idempotente ante re-ejecución. Verificación: `AltaConsultorioTest` sobre compose real da de alta un consultorio desde cero, comprueba `/actuator/health` `UP` y que no se crean usuario, consultorio ni credenciales de WhatsApp; RED→GREEN→TRIANGULAR (alta nueva + repetición)→REFACTOR.
- [x] 2.3 Verificar aislamiento entre instancias. Verificación: `AislamientoBasesTest` con PostgreSQL real demuestra que `consultorio_a_runtime` conecta a su base y NO puede conectar a `consultorio_b`, y que `PUBLIC` carece de CONNECT; RED→GREEN→TRIANGULAR (propia/cruzada/PUBLIC)→REFACTOR.

## 3. Compose, proxy y límites de recursos

- [x] 3.1 Adaptar `deploy/docker-compose.yml` para N instancias con Caddy TLS y subdominio por consultorio, sin publicar el puerto de PostgreSQL fuera de loopback en demo. Verificación: `ConfiguracionComposeTest` valida que cada consultorio enruta por HTTPS a su contenedor y que la base no queda expuesta; RED→GREEN→TRIANGULAR (dos consultorios)→REFACTOR.
- [x] 3.2 Extender límites de CPU/memoria a `postgres`, `migrate` y Caddy, conservando `cpus`/`mem_limit` en `app` y `DB_POOL_MAX_SIZE=5`. Verificación: `LimitesRecursosTest` inspecciona los cuatro servicios y el pool ≤ 5, y comprueba que un consumo excesivo no desplaza a la otra instancia; RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 3.3 Fijar la imagen por tag/digest semántico en el alta y en el rollout, sin construcciones ad hoc por instancia. Verificación: `ImagenVersionadaTest` comprueba que dos instancias informan el mismo tag/digest y que no se usa `latest`; RED→GREEN→TRIANGULAR→REFACTOR.

## 4. Migración por base alineada con C-02

- [x] 4.1 Mantener un servicio `migrate` efímero por base con Flyway `11.7.2`, `FLYWAY_PLACEHOLDERS_RUNTIME_ROLE` desde `APP_DB_USER`, y `app` con `SPRING_FLYWAY_ENABLED=false` y `depends_on: service_completed_successfully`. Verificación: `MigratePorBaseTest` sobre PostgreSQL real aplica V1/V2 con el placeholder correcto y confirma que la app no ejecuta Flyway; RED→GREEN→TRIANGULAR (dos bases)→REFACTOR.
- [x] 4.2 Automatizar la migración de todas las instancias de una versión a otra, deteniendo el proceso ante el primer fallo. Verificación: `MigrarTodasInstanciasTest` comprueba el mismo historial de versiones en cada base, el arranque posterior de cada app y que un fallo en una base impide continuar; RED→GREEN→TRIANGULAR (éxito + fallo)→REFACTOR.
- [x] 4.3 Garantizar no reescritura de migraciones aplicadas. Verificación: `HistorialMigracionesTest` confirma que V1/V2 no se editan, que no se reaplican y que `${runtime_role}` conserva el runtime de cada instancia; RED→GREEN→TRIANGULAR→REFACTOR.

## 5. Rollout canario y rollback

- [x] 5.1 Implementar el rollout gradual que actualiza primero los dos consultorios de demo y solo continúa si ambos verifican salud. Verificación: `RolloutCanarioTest` simula canaria sana y canaria con `/actuator/health` `DOWN`, y confirma STOP antes de tocar las demás; RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 5.2 Documentar y automatizar el rollback por vuelta al tag/digest anterior más restore del `pg_dump`, sin `flyway undo` y sin `UPDATE`/`DELETE`/`TRUNCATE` sobre `evento_auditoria`. Verificación: `RollbackAppendOnlyTest` sobre PostgreSQL real confirma filas de auditoría intactas y corrección registrada como evento nuevo; el runbook no contiene `flyway undo`; RED→GREEN→TRIANGULAR→REFACTOR.

## 6. Backups y restauración por consultorio

- [x] 6.1 Implementar `pg_dump` diario por base a directorio externo montado o destino configurable por variable, con retención ≥ 30 días y fallo explícito del job. Verificación: `BackupDiarioTest` valida un dump por base, política de 30 días y alerta ante fallo; RED→GREEN→TRIANGULAR (dos bases + fallo)→REFACTOR.
- [x] 6.2 Implementar y probar la restauración de un solo consultorio desde su dump, verificando roles y grants. Verificación: `RestoreUnConsultorioTest` restaura solo una base, confirma que la otra sigue intacta y comprueba owner, `REVOKE CONNECT FROM PUBLIC`, `CONNECT` del runtime y privilegios mínimos; RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 6.3 Escribir el runbook de backup/restore (incluida prueba trimestral y verificación de roles/grants) y la advertencia de solo datos ficticios (SU-07) con PA-13/PA-09 fuera de alcance. Verificación: el runbook existe, los comandos documentados coinciden con los scripts y no contiene secretos reales.

## 7. Logs, alertas y secretos

- [x] 7.1 Configurar logs JSON conservando la whitelist de MDC de C-02 y metadatos técnicos de correlación. Verificación: `LogsJsonSegurosTest` captura logs y afirma que son JSON, incluyen solo campos permitidos y no contienen `entidad_id`, PII ni datos clínicos; RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 7.2 Implementar las alertas técnicas mínimas: app caída/`DOWN` en `/actuator/health`, base caída y fallo de backup/migración, sin alerta de token de Meta. Verificación: `AlertasMinimasTest` dispara los tres escenarios y verifica que no aparece la alerta diferida a C-15/C-16; RED→GREEN→TRIANGULAR→REFACTOR.
- [x] 7.3 Documentar secretos por `env_file` separado y actualizar `.env.example` sin valores reales (`DB_POOL_MAX_SIZE`, `ADMIN_INITIAL_EMAIL`, `WA_VERIFY_TOKEN`). Verificación: `SecretosFueraRepoTest` confirma un env_file por instancia, variables sensibles vacías en el ejemplo y ausencia de tokens/claves en logs, alertas y workflow; RED→GREEN→TRIANGULAR→REFACTOR.

## 8. CI mínimo sin push (D10)

- [x] 8.1 Ajustar solo el job `imagen` de `../.github/workflows/global-sdd-ci.yml` para etiquetar la imagen con tag semántico y reportar su digest, sin push ni secretos. Verificación: el workflow sigue haciendo build local, expone el digest y no contiene credenciales ni comandos de publicación; revisión humana explícita aplicada por governance ALTO antes de ejecutar Apply sobre este archivo.

## 9. Verificación integral y cierre de planeamiento

- [x] 9.1 Ejecutar la suite completa y los cuatro tests obligatorios del roadmap (alta desde cero, conexión cruzada denegada, migrar todas las instancias, restaurar un solo consultorio) más los escenarios del delta `despliegue-y-operacion`. Verificación: todos verdes, `./gradlew check` y `npm test` en verde, sin H2/mocks de base y sin regresión de C-01/C-02.
- [x] 9.2 Verificar la matriz D1-D10 / C1-C10 y el freno de governance: solo artefactos y cambios previstos, sin specs publicadas ni migraciones modificadas, sin commit/push. Verificación: `git diff` revisado, `openspec validate despliegue-y-operacion --strict` en verde y aprobación humana registrada antes de cualquier Archive.

## Workflow follow-up

- Inspección del usuario de los cuatro artefactos antes de autorizar Apply (governance ALTO).
- Tras Apply y verificación, actualizar `CHANGES.md` y archivar el change solo con autorización explícita; C-36 conserva la parte productiva.
