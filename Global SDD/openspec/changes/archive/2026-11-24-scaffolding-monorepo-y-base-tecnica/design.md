# Design

## Context

- Motivación y alcance: ver `proposal.md`. Requisitos: ver `specs/plataforma-base/spec.md`.
- Hoy `Global SDD/` solo tiene documentación (`knowledge-base/`, `design-system/`, `discovery/`, `openspec/`). No hay código.
- `Global SDD/` es una **subcarpeta** del repositorio git `Desarrollo-De-Software` (GitHub), que también contiene otros TPs Maven (`TP Spring`, `TP02-JPA`, `0.ARCA`). El `.gitignore` raíz solo ignora `target/` y `*.class`.
- Entorno observado: JDK 21.0.8 y Node 24 instalados localmente.
- Restricciones que manda la KB: monolito modular por dominio con capas (08 §Patrones), estructura de directorios (08 §Estructura), sesión de servidor y misma origen (DD-08), instancia por consultorio con base propia y pool acotado (DD-01, 08 §Despliegue), demo en un VPS con Docker Compose, Caddy y PostgreSQL en contenedor (DD-11), UTC en base (RN-GL-02, 04 §Convenciones), Java 21 / Spring Boot 3 / PostgreSQL 16 (SU-06).
- Reglas duras de `AGENTS.md` que este diseño respeta: esquema solo por Flyway, sin secretos en el repo, Testcontainers y no H2, límites de módulo, `TIMESTAMPTZ` en UTC, DTOs + `ProblemDetail`, sin datos clínicos en logs, tokens del design system en el frontend, tests verdes para cerrar, sin commits sin pedido.

## Goals / Non-Goals

**Goals:**
- Que cualquier change posterior pueda agregar código a un módulo sin decidir de nuevo build, estructura, errores, tiempo, migraciones, empaquetado ni CI.
- Que la arquitectura del monolito modular sea verificada por el build, no por disciplina.
- Que `docker compose up` levante la app y PostgreSQL localmente con la misma imagen que usará la demo.

**Non-Goals:**
- Spring Security, login, CSRF o cabeceras de seguridad (C-03). En C-01 la única superficie HTTP es `/actuator/health`, la SPA estática y rutas de test.
- Tablas de dominio, auditoría o eventos (C-02 en adelante).
- Caddy, varios consultorios en el mismo compose, `pg_dump` diario, rollout y alta de consultorios (C-35).
- Pantallas, componentes visuales o rutas reales de la SPA.

## Decisions

### D1 — Gradle (Kotlin DSL, con Gradle Wrapper)
**Elección**: Gradle vía `gradlew`, proyecto de un solo módulo en `backend/` con `build.gradle.kts` y `settings.gradle.kts`. Plugins: `org.springframework.boot` (versión 3.5.x fijada), `io.spring.dependency-management` (el BOM de Spring Boot gestiona las versiones), `java` con **toolchain 21** y `jacoco`.
**Por qué**: **decisión del usuario (2026-11-24)**. Además aporta builds incrementales y caché de build más rápidos, útiles con 1-2 personas iterando a menudo (R-07).
**Alternativas**: Maven con Maven Wrapper (lo usan los otros TPs del repo: descartado por elección del usuario); Gradle multi-proyecto con un subproyecto por módulo de dominio (descartado: los límites se verifican con ArchUnit, no hace falta el costo de N builds).
**Consecuencias**: el comando de verificación del backend es `./gradlew build` (corre `check`: tests, arquitectura, Testcontainers y la verificación de JaCoCo). La salida queda en `backend/build/` y la caché en `backend/.gradle/`; ninguna de las dos está en el `.gitignore` raíz, que solo cubre `target/`, así que se agregan al `.gitignore` del proyecto (D15).

### D2 — ArchUnit en lugar de Spring Modulith para verificar límites
**Elección**: ArchUnit (`archunit-junit5`) con reglas propias en `backend/src/test/java/.../arquitectura/`.
**Por qué**: la regla del proyecto tiene dos ejes: **capas dentro de cada módulo** (`api → application → domain`, `infrastructure` implementa puertos) y **API pública entre módulos**. Spring Modulith verifica bien el segundo eje con su convención (paquete base público, subpaquetes internos), pero no verifica capas internas y obligaría a reorganizar la estructura `api/application/domain/infrastructure` de la KB o a anotar `@NamedInterface` en cada módulo. ArchUnit expresa ambos ejes con la estructura de la KB tal cual y es solo una dependencia de test.
**Alternativas**: Spring Modulith (se puede sumar más adelante solo por su registro de publicación de eventos, si C-02/C-15 lo necesitan, sin cambiar estas reglas); ArchUnit + Modulith juntos (redundante hoy).
**Reglas** (las fija C-01, con fixtures que prueban que fallan):
1. Módulos de dominio: `agenda`, `pacientes`, `clinica`, `caja`, `notificaciones`, `integraciones`, `identidad`, `auditoria`. Transversal: `shared`.
2. Capas por módulo: `api` puede usar `application` (y `domain` para tipos de valor); `application` puede usar `domain`; `domain` no usa `api`, `application` ni `infrastructure`; `infrastructure` puede usar `application` y `domain`; nadie del mismo módulo depende de `api` salvo `api`.
3. **API pública de un módulo = su paquete `<modulo>.application`** (servicios/fachadas, DTOs de entrada/salida, puertos y eventos publicados en `<modulo>.application.evento`). Un módulo no puede depender de `api`, `domain` ni `infrastructure` de otro módulo.
4. `shared` no depende de ningún módulo de dominio; todos pueden usar `shared`.
5. Sin ciclos entre módulos (`slices().should().beFreeOfCycles()`).
Como los paquetes nacen vacíos, cada uno lleva un `package-info.java` (para existir en git y documentar la capa) y se configura `archRule.failOnEmptyShould=false` en `archunit.properties`. Para que el test de arquitectura **demuestre** que falla ante una violación, se agregan clases fixture en un paquete de test aislado (`...arquitectura.fixture`) y tests que afirman que cada regla las rechaza; las reglas de producción excluyen ese paquete.

### D3 — Paquete base y estructura del backend
**Elección**: paquete base `ar.edu.utn.odonto` (neutral, sin el nombre del producto, que sigue abierto en PA-11). Clase de arranque `OdontoApplication` en ese paquete; `@EnableScheduling`, `@EnableJpaAuditing` y similares van en clases `@Configuration` dedicadas cuando un change las necesite, no en la clase de arranque.
```
backend/
├── gradlew, gradlew.bat, gradle/wrapper/
├── settings.gradle.kts
├── build.gradle.kts
└── src/
    ├── main/java/ar/edu/utn/odonto/
    │   ├── OdontoApplication.java
    │   ├── agenda/{api,application,domain,infrastructure}/package-info.java
    │   ├── … (mismo esquema para los 8 módulos de dominio)
    │   └── shared/
    │       ├── api/          (ManejadorGlobalDeErrores, SpaForwardController)
    │       ├── application/  (ZonaHorariaConsultorio, utilidades de tiempo)
    │       ├── domain/
    │       └── infrastructure/ (ConfiguracionTiempo: bean Clock, AppProperties)
    ├── main/resources/
    │   ├── application.yml
    │   ├── db/migration/V1__baseline.sql
    │   └── static/            (vacío en git; lo llena el build de la imagen)
    └── test/java/ar/edu/utn/odonto/
        ├── arquitectura/      (reglas ArchUnit + fixture/)
        ├── shared/…           (tests de errores, tiempo, SPA)
        └── support/           (contenedor PostgreSQL singleton, controlador de prueba)
```

### D4 — Versiones y gestión de dependencias
- Java 21 (`java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }` en `build.gradle.kts`).
- Spring Boot: última **3.5.x** estable al momento de apply, fijada explícitamente en el `parent` (nunca `latest`, nunca 4.x: la KB fija Spring Boot 3). Starters: `web`, `data-jpa`, `validation`, `actuator`; `flyway-core` + `flyway-database-postgresql`; driver `postgresql`.
- Sin versiones explícitas para artefactos gestionados por el BOM de Spring Boot (incluido Testcontainers y Flyway). Las únicas versiones explícitas son ArchUnit y el plugin JaCoCo, que el BOM no gestiona.
- Tests: `spring-boot-starter-test`, `spring-boot-testcontainers`, Testcontainers PostgreSQL y JUnit Jupiter alineados por el BOM, ArchUnit.
- Frontend: Node 24 LTS (fijado en `.nvmrc` y `engines`), versiones exactas en `package-lock.json`; React ≥ 18 (lo que traiga la plantilla oficial de Vite para React + TS, mínimo de la KB), TypeScript 5.

### D5 — Manejo de errores RFC 7807 en `shared`
- `spring.mvc.problemdetails.enabled=true` y un único `@RestControllerAdvice` (`ManejadorGlobalDeErrores`) que extiende `ResponseEntityExceptionHandler`, así los errores del framework (404 sin handler, 405, 415, JSON mal formado) salen como `ProblemDetail`.
- `MethodArgumentNotValidException` y `ConstraintViolationException` → `400` con propiedad `errores: [{campo, mensaje}]`; mensajes de Bean Validation en español (`ValidationMessages_es` / locale por defecto `es-AR`).
- `HttpMessageNotReadableException` → `400` con `detail` genérico ("El cuerpo de la petición no es válido"), sin el mensaje de Jackson.
- `Exception` no prevista → `500`, `detail` genérico, log con nivel ERROR del tipo de excepción y la ruta, sin cuerpo de la petición ni parámetros (Ley 25.326).
- `type` = `about:blank` para errores genéricos; los changes de dominio definirán sus propios `type` cuando los necesiten (por ejemplo el 409 de solapamiento de C-10).
- `server.error.include-stacktrace=never`, `include-message=never`.
- Para probar la validación sin endpoints de negocio se usa un controlador **solo de test** bajo `/api/v1/__test__/…` registrado en el contexto de test.

### D6 — Prefijo `/api/v1`
- Convención: cada controlador de negocio se anota con `@RequestMapping("/api/v1/<recurso>")` vía una constante `Api.V1 = "/api/v1"` en `shared.api`, en lugar de `server.servlet.context-path` (que también afectaría a `/actuator` y a la SPA) o de `PathMatchConfigurer.addPathPrefix` (más mágico y difícil de leer en cada controlador).
- `/actuator/health` queda fuera del prefijo, como lo nombra `CHANGES.md`.

### D7 — Tiempo y zona horaria
- Bean `Clock` = `Clock.systemUTC()` en `shared.infrastructure.ConfiguracionTiempo`; los tests lo reemplazan con `@TestConfiguration` / `@MockitoBean` por `Clock.fixed(...)`. Ningún código usa `Instant.now()` / `LocalDateTime.now()` sin `Clock` (se agrega una regla ArchUnit que prohíbe las llamadas `now()` sin argumentos de `java.time` fuera de `ConfiguracionTiempo`).
- `AppProperties` (`@ConfigurationProperties("app")`, `@Validated`): `baseUrl` (de `APP_BASE_URL`, obligatoria) y `timezone` (de `APP_TIMEZONE`, por defecto `America/Argentina/Buenos_Aires`, validada como `ZoneId`). Exposición: bean `ZonaHorariaConsultorio` con conversiones `Instant ↔ LocalDateTime/ZonedDateTime` en la zona del consultorio.
- Base en UTC: la JVM corre con zona `UTC` (`-Duser.timezone=UTC` en el `ENTRYPOINT` y `TZ=UTC` en la imagen; en tests, `tasks.test { jvmArgs("-Duser.timezone=UTC") }`) **y** además se fija `hibernate.jdbc.time_zone=UTC`. pgJDBC informa la zona de la JVM al abrir la sesión, así que `current_setting('TimeZone')` queda en `UTC`. Un test con la JVM en otra zona no es portable; el test verifica `current_setting('TimeZone')` con la configuración real.
- Jackson: `WRITE_DATES_AS_TIMESTAMPS=false`; `Instant` se serializa ISO-8601 con `Z`.
- Convención para changes futuros (documentada en `shared`): columnas de instante `TIMESTAMPTZ`, tipo Java `Instant` u `OffsetDateTime`; nunca `TIMESTAMP` sin zona.

### D8 — Flyway y base
- `V1__baseline.sql`: `CREATE EXTENSION IF NOT EXISTS btree_gist; … citext; … pgcrypto;`. Las tres son extensiones *trusted* desde PostgreSQL 13, así que puede crearlas el dueño de la base sin superusuario (relevante para el usuario por consultorio de DD-01).
- `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`, `spring.flyway.validate-on-migrate=true` (por defecto).
- Hikari: `maximum-pool-size` desde `DB_POOL_MAX_SIZE` con valor por defecto `5` (08 §Despliegue: pool ≤ 5 por instancia).
- Tests de integración: contenedor singleton `postgres:16.x-alpine` (tag exacto fijado en apply, mismo major que producción) con `@ServiceConnection`; prohibido H2 (no se agrega la dependencia).

### D9 — Actuator
- `management.endpoints.web.exposure.include=health`, `management.endpoint.health.show-details=never`, `show-components=never`. El indicador de la base viene incluido con JPA. Sin cambio de puerto de management (una sola superficie detrás de Caddy; C-35 puede restringir `/actuator` en el proxy).

### D10 — SPA servida por Spring Boot
- El build de Vite (`frontend/dist`) se copia en la imagen a `backend/src/main/resources/static/` **antes** de empaquetar el jar, así el jar contiene la SPA (una sola imagen, 08 §Estructura).
- `SpaForwardController` en `shared.api`: `GET` a rutas sin punto en el último segmento que no empiecen con `/api/` ni `/actuator` → `forward:/index.html`. Las rutas `/api/**` inexistentes siguen el camino de D5 (404 problem+json).
- En desarrollo: Vite en `:5173` con `server.proxy` de `/api` y `/actuator` hacia `http://localhost:8080`; no hay CORS porque en producción todo es mismo origen (DD-08).

### D11 — Frontend
- Generado con la plantilla oficial `react-ts` de Vite; estructura `src/features/`, `src/shared/` (`ui/`, `api-client/`, `formatos/`), `src/pages/`, cada carpeta con un `README.md` breve de una línea o un `index.ts` vacío para que exista.
- `vite.config.ts` con `defineConfig`, `@vitejs/plugin-react`, bloque `test` de Vitest (entorno `jsdom`, `setupFiles` con `@testing-library/jest-dom`), alias `@design-system` → `../design-system` y `server.fs.allow` para poder importar `design-system/tokens.css` (regla: no hardcodear colores).
- ESLint (flat config con `typescript-eslint`, `react-hooks`, `jsx-a11y`), Prettier + `eslint-config-prettier`. Scripts: `dev`, `build` (`tsc -b && vite build`), `lint`, `format:check`, `typecheck`, `test` (`vitest run`).
- Variables de entorno solo con prefijo `VITE_` y nunca secretos; C-01 no necesita ninguna.
- `App.tsx` queda como **placeholder técnico** (texto plano "Aplicación en construcción", sin estilos propios, importa `tokens.css`) solo para que exista un `index.html` servible y testeable. No es una pantalla del producto (ver `## UI Mockups`).

### D12 — Docker y Compose
- `deploy/Dockerfile`, multi-stage con build context = `Global SDD/` (necesita `frontend/`, `backend/` y `design-system/`):
  1. `frontend-build` (`node:24.x-alpine`, tag exacto): `package*.json` → `npm ci` → fuentes → `npm run build`.
  2. `backend-build` (`eclipse-temurin:21.x-jdk`, tag exacto): wrapper + `settings.gradle.kts` + `build.gradle.kts` → `./gradlew dependencies` (caché de dependencias) → fuentes → copiar `dist` a `src/main/resources/static/` → `./gradlew bootJar -x test` (los tests corren en CI, no en la imagen: Testcontainers necesita Docker).
  3. `runtime` (`eclipse-temurin:21.x-jre`, tag exacto): usuario no root, solo el jar, `TZ=UTC`, `EXPOSE 8080`, `HEALTHCHECK` contra `/actuator/health`, `ENTRYPOINT ["java","-Duser.timezone=UTC","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]`.
- `.dockerignore` en `Global SDD/` (excluye `node_modules`, `build`, `.gradle`, `dist`, `.env`, `knowledge-base`, `discovery`, `openspec`, `.git`).
- `deploy/docker-compose.yml` (**base compartida dev/demo**): servicios `app` (build de la imagen, `env_file: ../.env`, `depends_on: postgres: condition: service_healthy`, `security_opt: no-new-privileges`, healthcheck) y `postgres` (`postgres:16.x-alpine` pinneado, volumen nombrado `pgdata`, healthcheck `pg_isready`, script de init). Red propia `backend`. **Sin puertos de la base publicados** en el archivo base.
- `deploy/docker-compose.override.yml` (**solo dev**, Compose lo carga solo): publica `8080` de la app y `5432` de PostgreSQL en `127.0.0.1`.
- `deploy/postgres/init/01-base-consultorio.sh`: crea el usuario y la base de la instancia (`APP_DB_NAME`, `APP_DB_USER`, `APP_DB_PASSWORD`), con el usuario como dueño, y `REVOKE CONNECT ON DATABASE … FROM PUBLIC` (DD-01). La app nunca usa el superusuario `postgres`. C-35 extenderá este script a una base por consultorio y agregará `docker-compose.demo.yml` con Caddy y `pg_dump`.

### D13 — Variables de entorno
- `.env.example` en `Global SDD/` con **todas** las variables de 08 §Variables de entorno (incluidas las que usan changes futuros, marcadas "usada desde C-xx"), más las de compose (`POSTGRES_PASSWORD`, `APP_DB_NAME`, `APP_DB_USER`, `APP_DB_PASSWORD`) y `DB_POOL_MAX_SIZE`. Sensibles con valor vacío; no sensibles con ejemplos ficticios.
- `application.yml` solo referencia `${VAR}` / `${VAR:default}`; sin valores sensibles por defecto. `.env` queda en `.gitignore` del proyecto.
- C-01 usa: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_MAX_SIZE`, `APP_BASE_URL`, `APP_TIMEZONE`.

### D14 — CI con GitHub Actions
- Archivo `.github/workflows/global-sdd-ci.yml` **en la raíz del repo git** (un nivel arriba de `Global SDD/`): es el único lugar donde GitHub lee workflows. Disparadores `push`/`pull_request` con `paths: ['Global SDD/**', '.github/workflows/global-sdd-ci.yml']` para no correr con cambios de los otros TPs. `defaults.run.working-directory` con comillas por el espacio en la ruta.
- Jobs: `backend` (`actions/setup-java` Temurin 21 + `gradle/actions/setup-gradle` con caché; `./gradlew build`: unit, arquitectura, Testcontainers —Docker ya está en `ubuntu-latest`— y JaCoCo), `frontend` (`actions/setup-node` con `.nvmrc`; `npm ci`, `lint`, `format:check`, `typecheck`, `test`, `build`) y `imagen` (`docker build -f deploy/Dockerfile .`, sin push), que depende de los dos anteriores.
- Acciones fijadas a versión mayor explícita; sin secretos en C-01.
- JaCoCo: reporte siempre y `check` de 80 % de líneas sobre `src/main` (excluida la clase de arranque).

### D15 — `.gitignore` del proyecto
- `Global SDD/.gitignore`: `.env`, `node_modules/`, `frontend/dist/`, `backend/build/`, `backend/.gradle/`, `backend/src/main/resources/static/*` (excepto un `.gitkeep`), reportes de cobertura.

## Risks / Trade-offs

- [El workflow de CI queda fuera de `Global SDD/`, la raíz que OpenSpec declara editable] → Es un único archivo, requerido por GitHub; se pide confirmación explícita al usuario al aplicar la tarea correspondiente.
- [El BOM de Spring Boot 3.5 gestiona Testcontainers 1.x, mientras la skill del proyecto prefiere 2.x] → Se respeta la regla dura de no fijar versiones del BOM; se usa `@ServiceConnection` (independiente de la versión). La migración a 2.x llega con una futura subida de Spring Boot.
- [Paquetes vacíos hacen que ArchUnit pase "por vacío"] → `failOnEmptyShould=false` más fixtures que demuestran que cada regla falla ante una violación.
- [La imagen JRE puede no traer `curl`/`wget` para el `HEALTHCHECK`] → Verificarlo en apply; si falta, instalar `curl` en la etapa runtime o usar un chequeo con `java` mínimo; nunca omitir el healthcheck.
- [Tests de integración lentos o imposibles sin Docker] → Contenedor singleton reutilizado por toda la suite; Docker es prerrequisito documentado en el `README` del proyecto y está disponible en `ubuntu-latest`.
- [Ruta con espacios (`Global SDD`, `Desarrollo de Software`) rompe scripts] → Comillas en CI, Dockerfile y scripts; verificado ejecutando cada comando en el entorno local de Windows y en CI Linux.
- [JaCoCo al 80 % con muy poco código puede ser frágil] → El código de C-01 es pequeño y todo tiene tests; si una clase de configuración pura baja el número, se excluye explícitamente en `jacocoTestCoverageVerification` del `build.gradle.kts` con un comentario.
- [`index.html` placeholder confundido con una pantalla aprobada] → Marcado como placeholder técnico; la primera pantalla real requiere su mockup aprobado.

## Migration Plan

No hay sistema previo. Rollback = revertir los archivos agregados. El primer arranque contra una base vacía aplica `V1__baseline`; como es una base nueva no hay riesgo de datos.

## Open Questions

- **Nombre del producto (PA-11)**: no afecta a C-01 porque el paquete base (`ar.edu.utn.odonto`) y el `artifactId` (`odonto-backend`) son neutrales; si se quiere renombrar, conviene hacerlo antes de C-02.
- **¿Puede `domain` llevar anotaciones JPA?** Se decide con la primera entidad (C-02/C-03); las reglas de C-01 no restringen dependencias de frameworks dentro de `domain`.
- **Registro de imágenes para la demo** (GHCR u otro): lo decide C-35; C-01 solo construye la imagen en CI, sin push.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI) — 2026-11-24
- Mockups: no aplica
- Approval: no aplica
- Preview: no aplica
- Visual verify: no aplica

C-01 no tiene pantallas. El `App.tsx` placeholder (D11) es un artefacto técnico para servir y testear el `index.html`; no es una pantalla del producto. La primera pantalla real (por ejemplo, login en C-03) pasa por el gate de mockups de la política `system-and-mockups`.
