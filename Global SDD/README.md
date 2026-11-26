# Global SDD — Gestión odontológica

Web app para que un odontólogo independiente gestione agenda, pacientes, historia clínica y cobros. Cada consultorio corre su propia instancia (single-tenant) con su propia base PostgreSQL.

- Dominio y decisiones: [knowledge-base/](knowledge-base/) y [AGENTS.md](AGENTS.md).
- Roadmap de changes: [CHANGES.md](CHANGES.md). Sistema de diseño: [design-system/README.md](design-system/README.md).

## Estructura

```
backend/    Spring Boot 3.5 (Java 21, Gradle Kotlin DSL), monolito modular
frontend/   React + Vite + TypeScript (SPA)
deploy/     Dockerfile multi-stage, docker-compose (app + PostgreSQL 16)
.env.example   Variables de entorno (copiar a .env; .env no se commitea)
```

Este proyecto vive en la subcarpeta `Global SDD/` de un repositorio git más grande. **Todos los comandos de abajo se corren desde `Global SDD/`** y las rutas llevan comillas porque la ruta contiene espacios.

## Prerrequisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| JDK | 21 (Temurin) | backend (Gradle usa toolchain 21) |
| Node.js | 24 LTS (ver `frontend/.nvmrc`) | frontend |
| Docker | Docker Desktop / Engine con el daemon corriendo | **tests de integración (Testcontainers)**, imagen y compose |

No hace falta instalar Gradle: se usa el Gradle Wrapper (`backend/gradlew`).

## Backend

```bash
cd backend
./gradlew build          # compila, corre tests (unitarios, arquitectura, Testcontainers) y verifica JaCoCo >= 80 %
./gradlew test           # solo tests
./gradlew bootRun        # arranca la app (requiere las variables de entorno y una base PostgreSQL)
```

- Los tests de integración levantan PostgreSQL 16 con Testcontainers: **Docker debe estar corriendo**. No se usa H2.
- `bootRun` necesita `DB_URL`, `DB_USER`, `DB_PASSWORD` y `APP_BASE_URL` (ver `.env.example`); sin ellas el arranque falla nombrando la variable.
- En una base sin odontólogo, el primer arranque también necesita `ADMIN_INITIAL_EMAIL` y `ADMIN_INITIAL_PASSWORD` (contraseña temporal: se exige cambiarla en el primer login). Opcionales: `RATE_LIMIT_LOGIN_POR_MINUTO` (10) y `RATE_LIMIT_PUBLICO_POR_MINUTO` (60).
- Salud: `GET /actuator/health`. API REST de negocio bajo `/api/v1`. Errores en `application/problem+json`.
- Toda la API exige sesión salvo `/api/v1/auth/login`, `/api/v1/auth/csrf`, `/api/v1/public/**` y `/api/v1/webhooks/**`. La SPA pide `GET /api/v1/auth/csrf` y reenvía el valor de la cookie `XSRF-TOKEN` en la cabecera `X-XSRF-TOKEN` en cada `POST/PUT/PATCH/DELETE`.
- El esquema se crea solo con migraciones Flyway (`backend/src/main/resources/db/migration`). Nunca se edita una migración ya aplicada: se agrega `V{n}__descripcion.sql`.
- Reporte de cobertura: `backend/build/reports/jacoco/test/html/index.html`.

## Frontend

```bash
cd frontend
npm ci
npm run dev             # Vite en http://localhost:5173; proxy de /api y /actuator a http://localhost:8080
npm run lint && npm run format:check && npm run typecheck
npm test                # Vitest
npm run build           # genera frontend/dist
```

Los colores, espaciados y tipografías salen de `design-system/tokens.css` (no se hardcodean; un test lo verifica). En C-01 `App.tsx` es solo un placeholder técnico, no una pantalla del producto.

## Docker y Compose (desarrollo local)

```bash
cp .env.example .env     # completar los valores marcados [SENSIBLE] con datos ficticios locales
docker build -f deploy/Dockerfile -t odonto:dev .
docker compose --env-file .env -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml up -d --build
curl http://127.0.0.1:8080/actuator/health      # {"status":"UP"}
docker compose --env-file .env -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml down
```

- `deploy/docker-compose.yml` es la base compartida dev/demo y **no publica puertos**. `deploy/docker-compose.override.yml` (solo desarrollo) publica `8080` y `5432` en `127.0.0.1`.
- La base y el usuario de la instancia los crea `deploy/postgres/init/01-base-consultorio.sh` la primera vez que se crea el volumen `pgdata`. Para empezar de cero: `down -v`.
- La demo (DD-11: Caddy, `pg_dump`, varios consultorios) la completa C-35.

## CI

`.github/workflows/global-sdd-ci.yml`, en la **raíz del repositorio git** (un nivel arriba de esta carpeta), porque GitHub solo lee workflows de ahí. Corre solo si cambia `Global SDD/**`: job `backend` (`./gradlew build`), `frontend` (lint, formato, tipos, tests, build) e `imagen` (`docker build`, sin push). La primera corrida en GitHub ocurre al hacer push.

## Reglas del proyecto

Ver la sección "Reglas Duras" de [AGENTS.md](AGENTS.md) (esquema solo por Flyway, sin secretos en el repo, UTC en base, `ProblemDetail`, límites entre módulos verificados con ArchUnit, etc.).
