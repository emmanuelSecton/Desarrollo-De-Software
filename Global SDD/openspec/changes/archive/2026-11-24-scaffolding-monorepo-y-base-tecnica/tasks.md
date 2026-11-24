# Tasks

> Rutas relativas a `Global SDD/` salvo indicación. Cada tarea cita entre corchetes las reglas duras de `AGENTS.md` que cumple. Sin commits salvo pedido explícito del usuario [Proceso: no commitear].

## 1. Esqueleto del backend y build

- [x] 1.1 Generar `backend/` con Gradle Wrapper, `settings.gradle.kts` y `build.gradle.kts` (toolchain Java 21, plugin `org.springframework.boot` 3.5.x fijado + `io.spring.dependency-management`, plugin `jacoco`, starters `web`, `data-jpa`, `validation`, `actuator`, `flyway-core`, `flyway-database-postgresql`, driver `postgresql`; test: `spring-boot-starter-test`, `spring-boot-testcontainers`, Testcontainers PostgreSQL/JUnit Jupiter sin versión propia, ArchUnit con versión explícita) y verificar que `./gradlew help` y `./gradlew dependencies --configuration runtimeClasspath` terminan sin errores (D1, D4) [Backend: sin H2]
- [x] 1.2 Crear `OdontoApplication` en `ar.edu.utn.odonto` y los 9 paquetes (`agenda`, `pacientes`, `clinica`, `caja`, `notificaciones`, `integraciones`, `identidad`, `auditoria`, `shared`) con subpaquetes `api/application/domain/infrastructure`, cada uno con `package-info.java` que describe la capa; verificar con un listado que existen los 36 `package-info.java` (D3)
- [x] 1.3 Crear `Global SDD/.gitignore` (`.env`, `node_modules/`, `frontend/dist/`, `backend/build/`, `backend/.gradle/`, `backend/src/main/resources/static/*` salvo `.gitkeep`, coverage) y verificar con `git status --ignored` que `.env` de prueba, `build/` y `.gradle/` quedan ignorados (D15) [Datos: no commitear secretos]

## 2. Límites entre módulos (ArchUnit)

- [x] 2.1 Escribir primero las clases fixture en `src/test/java/.../arquitectura/fixture/` (un `domain` que usa `infrastructure`, un módulo que usa `domain` de otro, `shared` que usa un módulo, un ciclo entre módulos, una llamada a `Instant.now()` sin `Clock`) y los tests que afirman que cada regla **falla** sobre ellas; verificar que esos tests están en rojo por falta de reglas (D2, D7) [Proceso: límites de módulo]
- [x] 2.2 Implementar las reglas ArchUnit de D2 (capas por módulo, API pública = `<modulo>.application`, `shared` sin dependencias de dominio, sin ciclos) y la regla de `now()` sin `Clock`, más `archunit.properties` con `archRule.failOnEmptyShould=false`; verificar que `./gradlew test --tests '*Arquitectura*'` pasa: las reglas aceptan el código real y rechazan cada fixture (spec: Límites entre módulos verificados automáticamente)

## 3. Configuración, base y salud

- [x] 3.1 Crear el soporte de tests: contenedor singleton `postgres:16.x-alpine` (tag exacto) con `@ServiceConnection` y perfil `test`; escribir el test de arranque de contexto (`@SpringBootTest`) y el test de `/actuator/health` = `200 {"status":"UP"}` sin `components`; verificar que fallan antes de la configuración (D8, D9) [Backend: Testcontainers, no H2]
- [x] 3.2 Escribir `application.yml` solo con `${VAR}` (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_MAX_SIZE:5`, `APP_BASE_URL`, `APP_TIMEZONE`), `ddl-auto=validate`, `open-in-view=false`, actuator con solo `health` y `show-details=never`, `include-stacktrace=never`; verificar que los tests de 3.1 pasan (spec: Endpoint de salud; Solo el endpoint de salud…) [Datos: no commitear secretos; Datos: esquema solo por Flyway]
- [x] 3.3 Agregar tests: `GET /actuator/env` y `/actuator/beans` → `404`; health `503 DOWN` al detener un contenedor PostgreSQL dedicado (no el singleton); verificar que pasan con `./gradlew build`
- [x] 3.4 Escribir primero el test de migración (base vacía → `V1__baseline` aplicada y `pg_extension` contiene `btree_gist`, `citext`, `pgcrypto`; segundo arranque sin migraciones pendientes), luego crear `db/migration/V1__baseline.sql` con `CREATE EXTENSION IF NOT EXISTS …`; verificar que el test pasa (D8; spec: Esquema base gestionado solo por migraciones) [Datos: esquema solo por Flyway, sin editar migraciones aplicadas]
- [x] 3.5 Escribir primero los tests de `AppProperties` (`@Validated`): arranque falla nombrando la variable si falta `APP_BASE_URL` o `DB_URL`, y si `APP_TIMEZONE` no es una zona válida; implementar `AppProperties` en `shared.infrastructure`; verificar que pasan (D7, D13; spec: Configuración por variables de entorno; Zona horaria configurable)

## 4. Tiempo y zona horaria (`shared`)

- [x] 4.1 Escribir primero los tests: bean `Clock` reemplazable por `Clock.fixed(2026-11-24T13:00:00Z)` en un test de contexto; `ZonaHorariaConsultorio` convierte `2026-11-24T13:00:00Z` a `2026-11-24T10:00` con la zona por defecto y vuelta; luego implementar `ConfiguracionTiempo` (`Clock.systemUTC()`) y `ZonaHorariaConsultorio`; verificar que pasan (D7; spec: Reloj único e inyectable; Zona horaria configurable) [Proceso: fechas en UTC]
- [x] 4.2 Escribir primero el test de integración que lee `current_setting('TimeZone')` por la conexión de la app y espera `UTC`, y el test de serialización JSON de un `Instant` como `"2026-11-24T13:00:00Z"`; luego configurar `hibernate.jdbc.time_zone=UTC`, `-Duser.timezone=UTC` en `tasks.test { jvmArgs(...) }` y Jackson `WRITE_DATES_AS_TIMESTAMPS=false`; verificar que pasan (spec: Fechas almacenadas y transmitidas en UTC) [Proceso: `TIMESTAMPTZ` en UTC]
- [x] 4.3 Documentar en `shared/package-info.java` (o `shared/README.md` dentro del paquete) la convención de tiempo para changes futuros (`TIMESTAMPTZ` + `Instant`/`OffsetDateTime`, siempre vía `Clock`, mostrar en la zona del consultorio); verificar que el texto cita RN-GL-02

## 5. Errores RFC 7807 y prefijo `/api/v1` (`shared`)

- [x] 5.1 Crear un controlador **solo de test** bajo `/api/v1/__test__` (POST con record validado, endpoint que lanza `RuntimeException`) y escribir primero los tests `@WebMvcTest`/MockMvc: `400` con `errores[{campo, mensaje}]` en español, JSON mal formado → `400` sin mensaje del parser, excepción → `500` genérico sin stack trace ni nombre de clase, `/api/v1/no-existe` → `404`, todos con `application/problem+json` y `type/title/status/detail/instance`; verificar que están en rojo (D5)
- [x] 5.2 Implementar `ManejadorGlobalDeErrores` (`@RestControllerAdvice` + `ResponseEntityExceptionHandler`), `spring.mvc.problemdetails.enabled=true`, mensajes de validación en español y la constante `Api.V1`; verificar que los tests de 5.1 pasan y que el log del 500 no incluye el cuerpo de la petición (spec: Formato uniforme de errores; Errores de validación; Errores inesperados; Prefijo de la API) [Backend: `ProblemDetail`, DTOs; Datos: sin datos clínicos en logs]

## 6. Frontend

- [x] 6.1 Generar `frontend/` con la plantilla oficial `react-ts` de Vite, fijar Node 24 LTS en `.nvmrc` y `engines`, crear `src/features/`, `src/shared/{ui,api-client,formatos}/`, `src/pages/`; verificar que `npm ci && npm run build` genera `dist/index.html` (D11)
- [x] 6.2 Configurar ESLint (flat config con `typescript-eslint`, `react-hooks`, `jsx-a11y`), Prettier + `eslint-config-prettier` y scripts `lint`, `format:check`, `typecheck`; verificar que los tres terminan sin errores
- [x] 6.3 Configurar en `vite.config.ts` el bloque `test` de Vitest (`jsdom`, setup con `@testing-library/jest-dom`), el proxy de `/api` y `/actuator` a `http://localhost:8080`, el alias `@design-system` y `server.fs.allow`; reemplazar `App.tsx` por el placeholder técnico que importa `design-system/tokens.css` sin estilos propios; escribir un test Vitest que lo renderiza; verificar que `npm test` pasa y que no hay colores hardcodeados (`grep` de `#[0-9a-f]{3,6}` y `rgb(` en `src/` vacío) [Frontend: tokens del design system]

## 7. SPA servida por Spring Boot

- [x] 7.1 Escribir primero los tests MockMvc con un `index.html` de prueba en el classpath de test: `GET /` y `GET /agenda/semana` → `200` con `index.html`; `GET /api/v1/no-existe` → `404 problem+json`; `GET /actuator/health` no es interceptado; luego implementar `SpaForwardController` en `shared.api` y dejar `src/main/resources/static/.gitkeep`; verificar que pasan (D10; spec: Entrega de la SPA desde el mismo origen)

## 8. Docker, Compose y variables de entorno

- [x] 8.1 Crear `.env.example` con todas las variables de `knowledge-base/08_arquitectura_propuesta.md` §Variables de entorno (marcando desde qué change se usan) más `DB_POOL_MAX_SIZE`, `POSTGRES_PASSWORD`, `APP_DB_NAME`, `APP_DB_USER`, `APP_DB_PASSWORD`, sensibles vacías; verificar con un `grep` que ninguna variable sensible tiene valor (D13; spec: Ejemplo sin secretos) [Datos: no commitear secretos]
- [x] 8.2 Crear `.dockerignore` y `deploy/Dockerfile` multi-stage (`frontend-build` → `backend-build` → `runtime` JRE no root, tags exactos, deps antes que fuentes, `TZ=UTC`, `HEALTHCHECK` a `/actuator/health`); verificar que `docker build -f deploy/Dockerfile -t odonto:dev .` termina, que `docker run --rm odonto:dev id -u` no es `0` y que el jar contiene `static/index.html` (D12)
- [x] 8.3 Crear `deploy/postgres/init/01-base-consultorio.sh` (usuario y base de la instancia, dueño de la base, `REVOKE CONNECT … FROM PUBLIC`), `deploy/docker-compose.yml` (app + postgres, red propia, volumen nombrado, healthchecks, `depends_on: service_healthy`, `no-new-privileges`, sin puerto de base publicado) y `deploy/docker-compose.override.yml` (puertos en `127.0.0.1`); verificar con `docker compose -f deploy/docker-compose.yml config` sin errores y que el archivo base no publica `5432`
- [x] 8.4 Verificar de punta a punta: copiar `.env.example` a `.env` con valores locales ficticios, `docker compose --env-file .env -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml up -d --build`, esperar `healthy`, comprobar `curl http://127.0.0.1:8080/actuator/health` = `UP`, `GET /` devuelve el `index.html` y el log de Flyway muestra `V1__baseline`; luego `docker compose down` (sin `-v` salvo que se quiera limpiar)

## 9. CI

- [x] 9.1 Pedir al usuario confirmación para crear un archivo fuera de `Global SDD/` y crear `../.github/workflows/global-sdd-ci.yml` (raíz del repo git) con filtros `paths`, `working-directory` entre comillas y jobs `backend` (`./gradlew build`, con `gradle/actions/setup-gradle`), `frontend` (`npm ci`, `lint`, `format:check`, `typecheck`, `test`, `build`) e `imagen` (`docker build`, sin push); verificar la sintaxis con `actionlint` si está disponible o validando el YAML, y documentar que la primera corrida verde en GitHub queda para cuando el usuario pida push (D14)
- [x] 9.2 Configurar JaCoCo (`jacocoTestReport` + `jacocoTestCoverageVerification` al 80 % de líneas, excluida `OdontoApplication`, enganchado a `check`) y verificar que `./gradlew build` local termina en verde con el umbral [Proceso: tests verdes para cerrar]

## 10. Documentación y cierre

- [x] 10.1 Escribir `Global SDD/README.md` (o sección en el existente si lo hubiera) con prerrequisitos (JDK 21, Node 24, Docker), cómo correr backend, frontend con proxy, tests y compose, y dónde vive el CI; verificar ejecutando cada comando documentado tal cual
- [x] 10.2 Verificación integral: `./gradlew build` en `backend/`, `npm run lint && npm run format:check && npm run typecheck && npm test && npm run build` en `frontend/` y `docker build` de la imagen, todo en verde; registrar la evidencia y marcar C-01 en `CHANGES.md` [Proceso: tests verdes para cerrar]

## Workflow follow-up

- Correr `openspec validate scaffolding-monorepo-y-base-tecnica --strict` antes de pedir revisión.
- Commit con Conventional Commits solo cuando el usuario lo pida; la primera corrida del workflow de CI ocurre recién al hacer push.
- Archivar el change con `openspec archive` tras la verificación, lo que crea `openspec/specs/plataforma-base/spec.md`.
