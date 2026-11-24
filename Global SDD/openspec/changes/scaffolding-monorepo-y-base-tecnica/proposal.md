# Proposal

## Why

Todavía no existe código: los 35 changes restantes del roadmap (`CHANGES.md`) dependen de una base técnica común. Sin un monorepo con límites de módulo verificados, migraciones Flyway, manejo uniforme de errores y del tiempo, imagen Docker y CI, cada change reinventaría esas piezas y el monolito modular (DD-02) se erosionaría desde el primer día. C-01 es el primer eslabón del camino crítico y no depende de nada.

## What Changes

- Monorepo dentro de `Global SDD/` con `backend/`, `frontend/` y `deploy/`.
- `backend/`: Java 21 + Spring Boot 3 con Gradle (Kotlin DSL) y Gradle Wrapper; paquetes vacíos por módulo (`agenda`, `pacientes`, `clinica`, `caja`, `notificaciones`, `integraciones`, `identidad`, `auditoria`, `shared`), cada uno con capas `api/application/domain/infrastructure`.
- Test de arquitectura que falla si un módulo rompe la regla de capas o usa clases internas de otro módulo.
- Módulo `shared` con: errores en formato RFC 7807 (`application/problem+json`), `Clock` inyectable, zona horaria del consultorio configurable (por defecto `America/Argentina/Buenos_Aires`), almacenamiento en UTC (RN-GL-02) y prefijo `/api/v1` para la API REST.
- Flyway con `V1__baseline.sql` que habilita `btree_gist`, `citext` y `pgcrypto`; Hibernate solo valida el esquema.
- Endpoint `/actuator/health` (incluye la base) sin exponer detalles internos.
- Spring Boot sirve el build de la SPA como estáticos, con fallback a `index.html` para las rutas del cliente.
- `frontend/`: Vite + React + TypeScript con estructura `features/ shared/ pages/`, ESLint, Prettier y Vitest; proxy de `/api` al backend en desarrollo.
- `deploy/Dockerfile` multi-stage (build de la SPA + build del backend + runtime JRE no root) y `deploy/docker-compose.yml` (app + PostgreSQL 16) para desarrollo, pensado como base del entorno de demo de DD-11 (C-35 lo completa).
- Configuración solo por variables de entorno, con `.env.example` sin valores reales.
- Pipeline de CI (GitHub Actions): build y tests del backend (incluye arquitectura y Testcontainers), lint/tests/build del frontend y build de la imagen.

## Capabilities

### New Capabilities

- `plataforma-base`: comportamiento técnico transversal que heredan todos los módulos: salud de la instancia, formato de errores de la API, prefijo de la API, manejo de fechas y zona horaria, configuración por entorno, esquema base de la base de datos, entrega de la SPA y verificación de los límites entre módulos.

### Modified Capabilities

Ninguna (no hay specs previas en `openspec/specs/`).

## Impact

- **Código nuevo**: `backend/`, `frontend/`, `deploy/`, `.env.example`, `.gitignore`/`.dockerignore` del proyecto y el workflow de CI. El workflow vive en la raíz del repositorio git (`../.github/workflows/`), porque `Global SDD/` es una subcarpeta del repo `Desarrollo-De-Software` y GitHub solo lee workflows desde la raíz.
- **Dependencias**: Spring Boot 3 (Web, Data JPA, Validation, Actuator), Flyway + driver PostgreSQL, ArchUnit, Testcontainers (PostgreSQL); Vite, React, TypeScript, ESLint, Prettier, Vitest, Testing Library.
- **Sistemas**: requiere Docker local y en CI (Testcontainers). PostgreSQL 16 como imagen pinneada.
- **No incluye**: Spring Security, autenticación ni RBAC (C-03), tablas de dominio (C-02 en adelante), Caddy, múltiples consultorios ni `pg_dump` (C-35), pantallas de UI.
