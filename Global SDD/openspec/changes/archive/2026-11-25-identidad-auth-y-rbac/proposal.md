# Proposal — identidad-auth-y-rbac (C-03)

> Governance: **CRÍTICO** (autenticación y autorización). Este documento es solo planificación: no se escribe código hasta que el usuario apruebe explícitamente la propuesta.

## Why

Hoy la API no tiene autenticación: cualquiera que llegue a la instancia puede invocar cualquier endpoint. C-03 está en el camino crítico (C-01 → C-02 → **C-03** → C-09 …) y abre el primer FORK del roadmap: C-05, C-07 y C-09 necesitan saber quién es el usuario y qué rol tiene. Además, la Ley 25.326 exige confidencialidad y mínimo privilegio sobre datos de salud (RN-SEG-02), lo que requiere RBAC antes de que exista cualquier dato clínico.

## What Changes

- Nueva tabla `usuario` (migración `V3`) con email `CITEXT` único, hash BCrypt, rol `ODONTOLOGO`/`SECRETARIA`, baja lógica, último login, marca de cambio de contraseña obligatorio, estado de bloqueo y la garantía en base de **exactamente un odontólogo activo**.
- Alta automática del odontólogo inicial al arrancar una instancia vacía, a partir de `ADMIN_INITIAL_EMAIL` y una contraseña temporal `ADMIN_INITIAL_PASSWORD`; debe cambiarla en el primer login.
- Endpoints de sesión bajo `/api/v1/auth`: `GET /csrf`, `POST /login`, `POST /logout`, `GET /me` y `POST /password` (cambio de la propia contraseña).
- Sesión de servidor con Spring Security (DD-08): cookie `HttpOnly`, `Secure`, `SameSite=Strict`, expiración por 30 min de inactividad (RN-SEG-04), protección CSRF para la SPA y rotación del identificador de sesión al iniciar sesión.
- BCrypt con cost 12; bloqueo de 15 min tras 5 intentos fallidos en 15 min (RN-SEG-03); respuesta de error idéntica para credenciales inválidas, usuario inexistente, inactivo o bloqueado (sin enumeración de cuentas).
- Mientras el usuario tenga el cambio de contraseña pendiente, solo puede usar `/auth/me`, `/auth/logout` y `/auth/password`; el resto responde `403`.
- RBAC: roles `ROLE_ODONTOLOGO` y `ROLE_SECRETARIA` con seguridad a nivel de método (`@PreAuthorize`) habilitada; todo `/api/v1/**` exige sesión salvo las rutas públicas de `03_actores_y_roles.md`.
- Rate limiting por IP (Bucket4j, en memoria) en `POST /api/v1/auth/login` y `/api/v1/public/**`, con `429` y `Retry-After`.
- Auditoría de `LOGIN` y `LOGIN_FALLIDO` con el módulo de C-02, sin email ni contraseña en el detalle.
- Cabeceras de seguridad: CSP, HSTS, `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options: DENY`.
- **BREAKING (comportamiento de la API)**: una ruta inexistente bajo `/api/v1` pedida **sin sesión** pasa a responder `401` en vez de `404`, para no revelar qué rutas existen. Con sesión sigue respondiendo `404`.

Fuera de alcance: recuperación de contraseña por email (C-04), ABM de usuarios y secretarias (C-05), pantallas de login y cambio de contraseña (C-06, requieren mockup), confianza en `X-Forwarded-For` detrás de un proxy (C-35), 2FA.

## Capabilities

### New Capabilities
- `identidad`: usuarios internos, autenticación por sesión, bloqueo por intentos, cambio de contraseña obligatorio, autorización por rol, rate limiting de endpoints sin sesión y cabeceras de seguridad.

### Modified Capabilities
- `plataforma-base`: el requisito "Prefijo de la API REST" cambia: una ruta inexistente bajo `/api/v1` responde `401` sin sesión y `404` con sesión.

## Impact

- **Backend**: módulo `identidad` (api, application, domain, infrastructure); configuración de seguridad en `shared/infrastructure`; nuevas dependencias `spring-boot-starter-security`, `spring-security-test` y `bucket4j-core`.
- **Base**: migración `V3__identidad.sql`; el rol runtime recibe `SELECT, INSERT, UPDATE` sobre `usuario` (no `DELETE`: la baja es lógica).
- **Configuración**: nuevas variables `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD` y límites opcionales de rate limiting en `.env.example` y en `deploy/docker-compose.yml`.
- **Tests existentes**: los tests de controladores de C-01 que llaman a `/api/v1` sin sesión deben autenticarse o adaptarse al `401`.
- **Frontend**: ninguno en este change; C-06 consume estos endpoints.
- **Desbloquea**: C-04, C-05, C-07, C-09.
