# CHANGES — Secuencia de Implementación

> Índice canónico de todos los changes del proyecto **Global SDD** (gestión de consultorio odontológico con anti-ausentismo por WhatsApp; single-tenant, una instancia por consultorio sobre host y clúster PostgreSQL compartidos).
> Actualizado el 2026-11-24: resolución de IN-01 (DD-01 revisada) e IN-02 (nuevo change C-37, DD-10); PA-01 a PA-04 resueltas: **1-2 personas, ~3 meses, demo** → C-30, C-31 y C-34 postergados a post-MVP (DD-13); plan replanificado para 2 personas.
> Cada change es atómico: un agente puede implementarlo en una sesión (~4-6 horas).
> **Leer este archivo antes de empezar un item.**

Stack: Java 21 + Spring Boot 3 (monolito modular) · React + Vite + TypeScript (SPA) · PostgreSQL 16 · WhatsApp Cloud API, Google Calendar, email.
Prioridades de la KB: **M** = MVP imprescindible, **S** = MVP deseable (se recorta si el plazo aprieta, PA-01). Los changes que cubren solo US de prioridad S están marcados `(S)` y quedan fuera del camino crítico.

> **Gate visual**: todo change marcado con **Requiere mockup HTML aprobado** toca UI. Antes de `apply` necesita un mockup HTML aprobado por el usuario (gate `design-mockups`, modo `system-and-mockups`, design system Revision 1 aprobado en `design-system/README.md`).

---

## Cómo usar este documento

1. **Identificar el item**: elegí un change con estado `[ ]` cuyas dependencias estén todas en `[x]` (ver el árbol y los GATES).
2. **Verificar dependencias y leer la KB**: confirmá con `openspec status` el estado real y leé los archivos de "Leer antes" del change.
3. **Acordar alcance y verificación**: proponé el change (`openspec propose`), confirmá el Scope con el usuario y definí cómo se verifica (tests, criterios de aceptación de las US). Si el change toca UI, primero el mockup HTML aprobado.
4. **Verificar el resultado**: correr tests, revisar los criterios de aceptación de las US y las reglas `RN-*` citadas; en changes de UI comparar contra el mockup aprobado.
5. **Actualizar estado**: marcar el change como `[x]` en este archivo, archivar el change en OpenSpec y guardar decisiones relevantes en engram.

Niveles de governance (ver reglas globales): **CRITICO** = análisis y aprobación humana explícita antes de escribir código; **ALTO** = proponer y esperar revisión; **MEDIO** = implementar con checkpoints; **BAJO** = autonomía completa si pasan los tests.

---

## Árbol de dependencias

```
C-01 scaffolding-monorepo-y-base-tecnica
 ├── C-35 despliegue-por-instancia-y-operacion (anticipable; solo requiere C-01)
 └── C-02 auditoria-append-only
      └── C-03 identidad-auth-y-rbac
           ├── C-04 email-adapter-y-recuperacion-password
           ├── C-05 configuracion-consultorio-y-prestaciones
           ├── C-06 gestion-usuarios-internos
           ├── C-07 frontend-shell-login-y-layout
           │     └── C-08 frontend-configuracion-usuarios-y-recuperacion   (también C-04, C-05, C-06)
           ├── C-28 caja-presupuestos-y-cobros                              (también C-05, C-09)
           └── C-09 pacientes-core                                          (también C-02)
                ├── C-23 historia-clinica-append-only
                │     └── C-24 odontograma-backend
                │           └── C-25 exportacion-pdf-historia-clinica
                └── C-10 agenda-turnos-core                                 (también C-05)
                      ├── C-11 bloqueos-y-disponibilidad
                      ├── C-12 asistencia-y-ausencias
                      ├── C-18 lista-de-espera-manual
                      ├── C-14 notificaciones-outbox-y-email              (también C-04)
                      │     ├── C-15 whatsapp-adapter-y-webhooks
                      │     │     ├── C-37 oferta-de-huecos-por-whatsapp           (también C-18)
                      │     │     └── C-16 recordatorios-y-confirmacion-whatsapp   (también C-14)
                      │     │           ├── C-17 turnos-sin-confirmar-bandeja
                      │     │           └── C-34 indicadores-de-ausentismo (S) — POSTERGADO post-MVP
                      │     └── C-20 reserva-online-api-publica              (también C-09, C-11)
                      │           └── C-21 gestion-turno-por-enlace
                      │                 └── C-22 frontend-reserva-publica    (también C-07, C-20)
                      ├── C-13 frontend-agenda                               (también C-07, C-11, C-12)
                      │     └── C-19 frontend-bandeja-y-lista-de-espera    (también C-17, C-18, C-37)
                      ├── C-26 frontend-ficha-paciente-y-evolucion         (también C-07, C-09, C-23)
                      │     └── C-27 frontend-odontograma-y-copia-hc       (también C-24, C-25)
                      └── C-31 google-calendar-sync (S) — POSTERGADO post-MVP

C-28 ──► C-29 frontend-caja                (también C-26)
C-28 ──► C-30 resumen-de-caja (S) — POSTERGADO post-MVP
C-28 + C-25 ──► C-33 exportacion-total-de-datos
C-02 + C-07 ──► C-32 consulta-de-auditoria

C-36 hardening-seguridad-y-cumplimiento-legal  ← cierra el MVP:
     C-16, C-19, C-22, C-27, C-29, C-32, C-33, C-35 (y C-21, C-12, C-06, C-08 por transitividad)
```

### Paralelismo por fase

```
GATE 0: C-01 ✓
  → C-02 auditoria-append-only                         [Agente A]
  → C-35 despliegue-por-instancia-y-operacion          [Agente C — anticipable, solo requiere C-01]

GATE 1: C-02 ✓
  → C-03 identidad-auth-y-rbac                         [Agente A]

GATE 2: C-03 ✓                                          ← FORK
  → C-09 pacientes-core                                [Agente A]
  → C-05 configuracion-consultorio-y-prestaciones      [Agente B]
  → C-07 frontend-shell-login-y-layout                 [Agente C]
  → C-04 email-adapter-y-recuperacion-password         [Agente B/C — cuando liberen]
  → C-06 gestion-usuarios-internos                     [Agente B/C — cuando liberen]
  → C-28 caja-presupuestos-y-cobros                    [Agente B — si C-05 ✓ y C-09 ✓]

GATE 3: C-05 ✓ + C-09 ✓
  → C-10 agenda-turnos-core                            [Agente A]
  → C-23 historia-clinica-append-only                  [Agente B — solo requiere C-09 ✓]

GATE 4: C-10 ✓                                          ← FORK
  → C-11 bloqueos-y-disponibilidad                     [Agente A]
  → C-12 asistencia-y-ausencias                        [Agente B]
  → C-18 lista-de-espera-manual                        [Agente B]
  → C-14 notificaciones-outbox-y-email                 [Agente A — si C-04 ✓]
  → C-13 frontend-agenda                               [Agente C — si C-07 ✓, C-11 ✓, C-12 ✓]

GATE 5: C-14 ✓
  → C-15 whatsapp-adapter-y-webhooks                   [Agente A]
  → C-20 reserva-online-api-publica                    [Agente B — si C-09 ✓, C-11 ✓]

GATE 6: C-23 ✓
  → C-24 odontograma-backend                           [Agente B]
  → C-26 frontend-ficha-paciente-y-evolucion           [Agente C — si C-07 ✓, C-10 ✓]

GATE 7: C-15 ✓ → C-16 recordatorios-y-confirmacion-whatsapp   [Agente A]
        C-15 ✓ + C-18 ✓ → C-37 oferta-de-huecos-por-whatsapp  [Agente B]
        C-24 ✓ → C-25 exportacion-pdf-historia-clinica        [Agente B]
        C-20 ✓ → C-21 gestion-turno-por-enlace                [Agente A/B]

GATE 8: C-16 ✓ → C-17 turnos-sin-confirmar-bandeja     [Agente A]
        C-25 ✓ + C-26 ✓ → C-27 frontend-odontograma-y-copia-hc [Agente C]
        C-21 ✓ → C-22 frontend-reserva-publica         [Agente C]

GATE 9: C-17 ✓ + C-18 ✓ + C-37 ✓ + C-13 ✓ → C-19 frontend-bandeja-y-lista-de-espera [Agente C]
        C-28 ✓ + C-26 ✓ → C-29 frontend-caja           [Agente C]
        C-28 ✓ + C-25 ✓ → C-33 exportacion-total-de-datos [Agente A]
        C-02 ✓ + C-07 ✓ → C-32 consulta-de-auditoria  [Agente B]

GATE 10: C-16, C-19, C-22, C-27, C-29, C-32, C-33, C-35 ✓
  → C-36 hardening-seguridad-y-cumplimiento-legal      [Agente A + revisión humana]
```

### Camino crítico (11 changes — mínimo irreducible)

```
C-01 → C-02 → C-03 → C-09 → C-10 → C-14 → C-15 → C-16 → C-17 → C-19 → C-36
```

Ramas casi críticas (10 pasos, mismo largo ±1): `C-10 → C-11 → C-20 → C-21 → C-22 → C-36` (reserva online) y `C-09 → C-23 → C-24 → C-25 → C-33 → C-36` (historia clínica y exportación). Cualquier atraso en ellas pasa a ser crítico. Fuera del camino crítico y postergados a post-MVP (DD-13): C-30, C-31 y C-34. C-37 (oferta de huecos) corre en paralelo a C-16 → C-17 y no alarga el camino crítico, pero C-19 la espera.

### Plan para 2 personas (~3 meses, PA-01)

Recorte aplicado (DD-13): C-30, C-31 y C-34 quedan **postergados a post-MVP** y fuera del plan. Quedan 34 changes en 19 pasos; con ~13 semanas, eso es aproximadamente un paso cada 4-5 días hábiles (riesgo R-07).

| Paso | Persona A (Backend) | Persona B (Frontend + backend auxiliar) |
|------|---------------------|-----------------------------------------|
| 1 | C-01 | — |
| 2 | C-02 | C-35 (entorno de demo, DD-11) |
| 3 | C-03 | — |
| 4 | C-09 | C-05 |
| 5 | C-10 | C-07 |
| 6 | C-04 | C-06 |
| 7 | C-14 | C-11 |
| 8 | C-15 | C-12 |
| 9 | C-16 | C-18 |
| 10 | C-17 | C-13 |
| 11 | C-37 | C-08 |
| 12 | C-20 | C-23 |
| 13 | C-21 | C-19 |
| 14 | C-24 | C-22 |
| 15 | C-25 | C-26 |
| 16 | C-28 | C-27 |
| 17 | C-33 | C-29 |
| 18 | C-32 | — |
| 19 | C-36 | — |

Notas del plan:
- Los pasos 1 a 3 son secuenciales; mientras tanto, la persona B prepara el entorno de demo (C-35) y los mockups de C-07 y C-13.
- **Con 1 sola persona**: ejecutar el orden de la columna A intercalando la B (34 pasos), recortar además C-32 y reducir C-36 a lo indispensable para la demo (R-07).
- Los changes postergados (C-30, C-31 y C-34) se retoman después de la demo sin afectar al resto.

---

## FASE 0 — Fundación técnica

### [C-01] `scaffolding-monorepo-y-base-tecnica`
- **Estado**: `[x]` completado (apply y archivo: 2026-11-24 → `openspec/changes/archive/2026-11-24-scaffolding-monorepo-y-base-tecnica`)
- **Scope**:
  - Monorepo `backend/` (Gradle o Maven, Java 21, Spring Boot 3) con paquetes por módulo vacíos: `agenda`, `pacientes`, `clinica`, `caja`, `notificaciones`, `integraciones`, `identidad`, `auditoria`, `shared` y capas `api/application/domain/infrastructure`
  - Verificación de límites de módulo con ArchUnit o Spring Modulith (test que falla si un módulo rompe la regla de capas)
  - `frontend/` con Vite + React + TypeScript, estructura `features/ shared/ pages/`, ESLint, Prettier, Vitest
  - Flyway con `V1__baseline.sql`: extensiones `btree_gist`, `citext`, `pgcrypto`
  - `deploy/Dockerfile` (build de la SPA servida como estáticos por Spring Boot) y `deploy/docker-compose.yml` (app + postgres) para dev
  - `shared`: manejo uniforme de errores (RFC 7807), `Clock` inyectable, zona horaria `America/Argentina/Buenos_Aires`, utilidades de tiempo (UTC en base, RN-GL-02), prefijo `/api/v1`
  - Configuración por variables de entorno (`DB_URL`, `APP_BASE_URL`, etc.) y `.env.example` sin valores reales
  - Pipeline CI: build + tests backend y frontend + ArchUnit
  - Tests: arranque del contexto con Testcontainers PostgreSQL, endpoint `/actuator/health`
- **Dependencias**: ninguna
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones aplicados, §Estructura de directorios, §Variables de entorno
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-01, DD-02, DD-03, SU-06
  - `knowledge-base/04_modelo_de_datos.md` §Convenciones

---

## FASE 1 — Identidad, auditoría y configuración

> Tras C-03 se abre el primer FORK. C-05, C-07 y C-09 son independientes entre sí.

### [C-02] `auditoria-append-only`
- **Estado**: `[x]` completado (apply y archivo: 2026-11-25 → `openspec/changes/archive/2026-11-25-auditoria-append-only`)
- **Scope**:
  - Migración Flyway: tabla `evento_auditoria` (`id BIGSERIAL`, `ocurrido_at`, `actor_tipo`, `actor_id`, `accion`, `entidad`, `entidad_id`, `detalle JSONB`, `ip INET`)
  - Append-only: trigger que rechaza UPDATE/DELETE y rol de base de la app sin esos permisos (RN-GL-01)
  - Módulo `auditoria`: `AuditoriaService.registrar(...)` y listener de eventos de dominio in-process (`@TransactionalEventListener(AFTER_COMMIT)`)
  - Catálogo de acciones (`LOGIN`, `LOGIN_FALLIDO`, `VER_HISTORIA`, `CREAR_TURNO`, `EXPORTAR`, …) como enum en código
  - Tests: intento de UPDATE/DELETE falla a nivel de base, registro de actor sistema/paciente/usuario
- **Dependencias**: `C-01`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §evento_auditoria
  - `knowledge-base/05_reglas_de_negocio.md` §Excepciones globales (RN-GL-01), RN-SEG-05
  - `knowledge-base/12_cumplimiento_legal.md` §Cómo responde el diseño (trazabilidad)
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (append-only, eventos de dominio)

### [C-03] `identidad-auth-y-rbac`
- **Estado**: `[x]` completado (apply y archivo: 2026-11-25 → `openspec/changes/archive/2026-11-25-identidad-auth-y-rbac`; PR #5)
- **Scope**:
  - Migración: tabla `usuario` (`email CITEXT UNIQUE`, `password_hash`, `rol ENUM('ODONTOLOGO','SECRETARIA')`, `activo`, `ultimo_login`, flag de cambio de contraseña obligatorio) con constraint de exactamente un `ODONTOLOGO` activo
  - Seed del odontólogo inicial desde `ADMIN_INITIAL_EMAIL` con contraseña temporal; forzar cambio en el primer login (US-001, parte de identidad)
  - `POST /api/v1/auth/login`, `POST /auth/logout`, `GET /auth/me`; sesión de servidor Spring Security (cookie `HttpOnly`, `Secure`, `SameSite=Strict`), token CSRF para la SPA (DD-08)
  - BCrypt cost ≥ 12; bloqueo tras 5 intentos fallidos en 15 min por 15 min (RN-SEG-03); expiración a los 30 min de inactividad (RN-SEG-04)
  - RBAC con `@PreAuthorize` por rol; matriz de permisos aplicada a rutas (la secretaria recibe 403 en rutas de historia clínica, RN-SEG-02)
  - Rate limiting por IP en `/auth/login` y `/public/**` (Bucket4j)
  - Auditoría de `LOGIN` y `LOGIN_FALLIDO` (usa C-02)
  - Tests: bloqueo, expiración, 403 por rol, CSRF, forzado de cambio de contraseña
- **Dependencias**: `C-01, C-02`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/03_actores_y_roles.md` §RBAC, §Rutas públicas
  - `knowledge-base/05_reglas_de_negocio.md` §Autenticación y seguridad (RN-SEG-01 a RN-SEG-05)
  - `knowledge-base/07_flujos_principales.md` Flujo 6
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-08

### [C-04] `email-adapter-y-recuperacion-password`
- **Estado**: `[x]` completado (apply y archivo: 2026-11-26 → `openspec/changes/archive/2026-11-26-email-adapter-y-recuperacion-password`; PR #10)
- **Scope**:
  - `EmailAdapter` SMTP detrás de un puerto (`MensajeriaPort` para email; variables `SMTP_*`, `MAIL_FROM`) con plantillas de texto/HTML simples y envío síncrono con reintento acotado
  - Migración: tabla de tokens de recuperación (hash del token, vencimiento 1 h, un solo uso)
  - `POST /api/v1/auth/password/recover` y `POST /auth/password/reset`; respuesta idéntica exista o no el email (sin enumeración de usuarios)
  - Auditoría del pedido y del cambio de contraseña
  - Decisión abierta PA-10 (proveedor de email transaccional): el adapter se mantiene agnóstico del proveedor
  - Tests: token vencido, token reutilizado, sin enumeración, doble de email en tests
- **Dependencias**: `C-03`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-003
  - `knowledge-base/05_reglas_de_negocio.md` RN-SEG-03, RN-SEG-04
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Adapter / puerto), §Variables de entorno
  - `knowledge-base/10_preguntas_abiertas.md` PA-10

### [C-05] `configuracion-consultorio-y-prestaciones`
- **Estado**: `[x]` completado (apply y archivo: 2026-11-26 → `openspec/changes/archive/2026-11-26-configuracion-consultorio-y-prestaciones`; PR #12)
- **Scope**:
  - Migración: `consultorio` (singleton con constraint de una sola fila, `slug_reserva UNIQUE`, `recordatorio_horas_antes=48`, `confirmacion_limite_horas=24`, `cancelacion_minima_horas=24`), `horario_atencion` (`hora_desde < hora_hasta`), `prestacion` (`duracion_minutos > 0 AND % 5 = 0`, `reservable_online`, `activa`)
  - Seed: consultorio desde la instalación, horarios lunes a viernes 9-13 y 15-19, prestaciones de ejemplo (Consulta 30, Limpieza 45, Obturación 45, Endodoncia 90, Extracción 45, Control 15)
  - Endpoints: `GET/PUT /api/v1/consultorio`, CRUD `/api/v1/horarios`, CRUD `/api/v1/prestaciones` (odontólogo CRUD, secretaria solo lectura)
  - Validaciones de parámetros del recordatorio/confirmación/cancelación (US-001) y duración múltiplo de 5 (RN-AG-02)
  - Auditoría de cambios de configuración
  - Tests: singleton, constraints de duración y horarios, permisos por rol
- **Dependencias**: `C-03`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-001
  - `knowledge-base/04_modelo_de_datos.md` §consultorio, §horario_atencion, §prestacion, §Seed data inicial
  - `knowledge-base/03_actores_y_roles.md` §RBAC (Configuración y prestaciones)
  - `knowledge-base/05_reglas_de_negocio.md` RN-AG-04, RN-NO-01, RN-NO-05, RN-TU-02

### [C-06] `gestion-usuarios-internos`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Endpoints `GET/POST /api/v1/usuarios`, `PATCH /usuarios/{id}` (alta con email y contraseña temporal, baja lógica `activo=false`), solo odontólogo
  - Cambio de contraseña forzado en el primer login de secretarias nuevas
  - Test de autorización de recursos: secretaria sin acceso a `/usuarios` ni a rutas de historia clínica (403, RN-SEG-02)
  - No se puede dar de baja ni cambiar el rol del único odontólogo activo
  - Auditoría de alta, baja y cambios de rol
  - Tests: matriz RBAC completa de la tabla de `03_actores_y_roles.md` sobre los recursos ya existentes
- **Dependencias**: `C-03`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-002
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `knowledge-base/05_reglas_de_negocio.md` RN-SEG-01, RN-SEG-02
  - `knowledge-base/04_modelo_de_datos.md` §usuario

### [C-07] `frontend-shell-login-y-layout`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: login, layout general y navegación, estados de error/sesión expirada, a 390x844 y 1440x900):
  - SPA base: router, `shared/api-client` con CSRF y manejo de 401/403, contexto de sesión, guards por rol (el menú de historia clínica no existe para la secretaria)
  - Importación de `design-system/tokens.css` y componentes base (botón, input, tabla, modal, toast, estados vacíos/carga/error)
  - Páginas: login, cambio de contraseña forzado, layout con navegación por rol, 404/403
  - Expiración por inactividad (30 min) con aviso
  - Tests: Vitest de guards y api-client; accesibilidad básica de formularios
- **Dependencias**: `C-03`
- **Governance**: BAJO
- **Leer antes**:
  - `design-system/README.md` (Revision 1 aprobada) y `design-system/tokens.css`
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura de directorios (frontend), §Seguridad
  - `knowledge-base/03_actores_y_roles.md` §RBAC, §Rutas públicas
  - `knowledge-base/09_decisiones_y_supuestos.md` SU-02, SU-05

### [C-08] `frontend-configuracion-usuarios-y-recuperacion`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: asistente de configuración, ABM de prestaciones/horarios, usuarios, recuperación de contraseña):
  - Asistente de configuración inicial en el primer login (datos del consultorio, horarios, prestaciones, horas de recordatorio/confirmación/cancelación)
  - Pantallas de edición de consultorio, horarios y prestaciones (duración múltiplo de 5, marca "reservable online")
  - Pantalla de usuarios internos (alta/baja)
  - Pantallas "olvidé mi contraseña" y "restablecer" (consumen C-04)
  - Validación espejo en frontend; tests de formularios
- **Dependencias**: `C-04, C-05, C-06, C-07`
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-001, US-002, US-003
  - `knowledge-base/04_modelo_de_datos.md` §consultorio, §prestacion
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura (frontend features)
  - `design-system/README.md`

---

## FASE 2 — Pacientes y agenda

> C-10 es el modelo core de turnos: todo lo de notificaciones, reserva y lista de espera depende de él. C-23 (historia clínica) puede avanzar en paralelo apenas termina C-09.

### [C-09] `pacientes-core`
- **Estado**: `[x]` completado (apply y archivo: 2026-11-26 → `openspec/changes/archive/2026-11-26-pacientes-core`; PR #7)
- **Scope**:
  - Migración: tabla `paciente` (`dni UNIQUE`, `telefono_e164`, `acepta_whatsapp`, `consentimiento_datos_at`, `ausencias_count`, `activo`), índices `dni`, `(apellido, nombre)`, `telefono_e164`
  - Endpoints: `GET /api/v1/pacientes?q=` (búsqueda por DNI, nombre o teléfono), `POST`, `GET/PUT /pacientes/{id}`; baja lógica (`activo`), nunca borrado físico (RN-HC-03)
  - Alta/upsert por DNI para uso interno y público: si el DNI existe se asocia sin sobrescribir datos clínicos
  - Normalización de teléfonos a E.164; registro de consentimiento con fecha (RN-DP-01)
  - Secretaria: CRU de datos personales y contacto; sin acceso a nada clínico
  - Auditoría de alta y modificación de pacientes (RN-GL-01)
  - Tests: unicidad por DNI, búsqueda, normalización E.164, permisos
- **Dependencias**: `C-02, C-03`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §paciente
  - `knowledge-base/05_reglas_de_negocio.md` RN-DP-01, RN-DP-02, RN-HC-03
  - `knowledge-base/03_actores_y_roles.md` §RBAC (Pacientes)
  - `knowledge-base/12_cumplimiento_legal.md` §Cómo responde el diseño (consentimiento, minimización)

### [C-10] `agenda-turnos-core`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Migración: tabla `turno` con `estado`, `origen`, `sobreturno`, `reprogramado_de`, `token_publico` (256 bits), `sin_confirmar_alertado`, `google_event_id`, `cancelado_por`, `motivo_cancelacion`, `version` (optimistic locking)
  - **Exclusion constraint** `EXCLUDE USING gist (tstzrange(inicio, fin) WITH &&) WHERE (estado IN ('RESERVADO','CONFIRMADO') AND NOT sobreturno)` (RN-AG-01); índices `(inicio)`, `(estado, inicio)`
  - Máquina de estados de RN-TU-01 con transiciones válidas; los estados finales solo se corrigen con rol interno y auditoría
  - Endpoints internos: `POST /api/v1/turnos`, `PATCH /turnos/{id}` (mover = reprogramar, RN-TU-03), `POST /turnos/{id}/cancelar` (pide motivo), creación de sobreturno con confirmación explícita (RN-AG-07)
  - Duración = la de la prestación, extensible en bloques de 5 min (RN-AG-02); conflicto de solapamiento → HTTP 409 con mensaje claro
  - Eventos de dominio `TurnoCreado`, `TurnoCancelado`, `TurnoReprogramado`, `TurnoEstadoCambiado` (`AFTER_COMMIT`); auditoría `CREAR_TURNO`, etc.
  - Decisión abierta PA-06 (sobreturnos en MVP): se implementa según RN-AG-07 y se valida con usuarios
  - Tests: concurrencia (dos inserts simultáneos → uno falla con 409), transiciones inválidas, reprogramación sin doble turno activo, sobreturno
- **Dependencias**: `C-05, C-09`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §turno
  - `knowledge-base/05_reglas_de_negocio.md` §Agenda (RN-AG-01, 02, 07), §Turnos y ciclo de vida (RN-TU-01 a 05)
  - `knowledge-base/06_funcionalidades.md` US-011
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (eventos de dominio, optimistic locking + exclusion constraint)
  - `knowledge-base/10_preguntas_abiertas.md` PA-06

### [C-11] `bloqueos-y-disponibilidad`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Migración: tabla `bloqueo_agenda` (`inicio`, `fin`, `motivo`, `creado_por`)
  - CRUD `/api/v1/bloqueos`; rechazo si superpone turnos activos (RN-AG-03, validación en servicio) → hay que cancelar o mover antes
  - Servicio de disponibilidad: huecos = `horario_atencion` − bloqueos − turnos activos − anticipación mínima de 2 h, hasta 60 días, solo prestaciones `reservable_online` para el canal público (RN-AG-04, RN-AG-05)
  - Endpoint interno `GET /api/v1/agenda/disponibilidad?prestacion&desde` reutilizable por C-20
  - Zona horaria del consultorio (RN-GL-02); casos de borde: cambio de franja, bloqueo parcial de un hueco
  - Tests: cálculo de huecos con bloqueos y turnos, anticipación, límite de 60 días
- **Dependencias**: `C-10`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-012
  - `knowledge-base/05_reglas_de_negocio.md` RN-AG-03, RN-AG-04, RN-AG-05
  - `knowledge-base/07_flujos_principales.md` Flujo 1 (cálculo de huecos)
  - `knowledge-base/04_modelo_de_datos.md` §bloqueo_agenda

### [C-12] `asistencia-y-ausencias`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Endpoints `POST /api/v1/turnos/{id}/atendido` y `/ausente` (secretaria y odontólogo)
  - `AUSENTE` incrementa `paciente.ausencias_count` de forma transaccional (RN-TU-04); corregir un estado final revierte el contador y queda auditado (US-013)
  - Las ausencias solo se registran; no limitan la reserva online en v1 (RN-TU-05); `GET` de pacientes y de agenda exponen el contador
  - Tests: incremento/decremento al corregir, transición inválida, auditoría de corrección
- **Dependencias**: `C-10`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-013
  - `knowledge-base/05_reglas_de_negocio.md` RN-TU-01, RN-TU-04, RN-TU-05
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-06

### [C-13] `frontend-agenda`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: vista día, vista semana, alta/mover/cancelar turno, bloqueos, marcar asistencia/ausencia, a 390x844 y 1440x900):
  - `features/agenda`: vista diaria y semanal con color por estado, marca "sin confirmar" y "paciente con ausencias", bloqueos y franjas fuera de horario (US-010)
  - Alta de turno con búsqueda de paciente por DNI/nombre/teléfono o alta en el momento; rechazo de solapamiento con mensaje claro y opción de sobreturno (US-011)
  - Mover (drag o diálogo) y cancelar con motivo; creación de bloqueos (US-012); marcar atendido/ausente (US-013)
  - Manejo de 409 por concurrencia (optimistic locking)
  - Tests de componentes y de flujos de alta/mover/cancelar
- **Dependencias**: `C-07, C-10, C-11, C-12`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-010 a US-013
  - `knowledge-base/05_reglas_de_negocio.md` RN-AG-01, RN-AG-07, RN-TU-01
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura (frontend features)
  - `design-system/README.md`

---

## FASE 3 — Notificaciones y anti-ausentismo (diferencial del producto)

> C-18 (lista de espera) no depende de notificaciones y se puede hacer en paralelo con C-14/C-15.

### [C-14] `notificaciones-outbox-y-email`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Migración: tabla `notificacion` (outbox) con índice único `(turno_id, tipo, canal)` (RN-NO-04), estados `PENDIENTE/ENVIADA/ENTREGADA/LEIDA/FALLIDA/RESPONDIDA`, `intentos`, `programada_para`
  - Puerto `MensajeriaPort` con `EmailAdapter` (de C-04) como primer canal; dispatcher con reintentos y backoff
  - Infraestructura de jobs programados (`@Scheduled`, configurable) reutilizada por C-16 y C-17
  - Listeners `AFTER_COMMIT` de `TurnoCreado`, `TurnoCancelado`, `TurnoReprogramado` que encolan `CONFIRMACION_RESERVA`, `CANCELACION`, `REPROGRAMACION` por email (el turno nunca se revierte por un fallo de envío)
  - Tests: idempotencia, reintentos, fallo del proveedor sin revertir el turno
- **Dependencias**: `C-04, C-10`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §notificacion
  - `knowledge-base/05_reglas_de_negocio.md` RN-NO-03, RN-NO-04, RN-NO-08
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (outbox, adapter, eventos)
  - `knowledge-base/07_flujos_principales.md` Flujo 1 (paso 6, casos de error)

### [C-15] `whatsapp-adapter-y-webhooks`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - `WhatsAppCloudAdapter` (Cloud API de Meta, credenciales leídas de `integracion_whatsapp`): envío de plantillas `recordatorio_turno`, `confirmacion_reserva`, `turno_cancelado`, `turno_reprogramado`, `oferta_hueco` y mensajes de servicio dentro de la ventana de 24 h
  - `GET /api/v1/webhooks/whatsapp` (verificación con `WA_VERIFY_TOKEN`) y `POST` con validación `X-Hub-Signature-256` (HMAC-SHA256, comparación en tiempo constante, `WA_APP_SECRET`), HTTP 200 inmediato y procesamiento asíncrono
  - Actualización de estados `sent/delivered/read/failed` sobre `notificacion`; idempotencia por `message.id`/`status.id`
  - Respuesta "BAJA" → `acepta_whatsapp=false`; texto libre se guarda sin cambiar el estado del turno
  - Degradación: token vencido/plantilla rechazada → alerta en logs y panel, respaldo por email; límite de envío → backoff
  - PA-04 resuelta (DD-12): cada odontólogo da de alta su WABA. Pantalla/endpoint `PUT /api/v1/integraciones/whatsapp` (solo odontólogo) que guarda `phone_number_id`, token y App Secret **cifrados** en `integracion_whatsapp`, valida contra la Graph API y muestra el estado de las plantillas; guía paso a paso en la ayuda
  - Demo: número de prueba gratuito de la Cloud API con destinatarios registrados
  - Someter **temprano** a aprobación de Meta todas las plantillas, en especial `oferta_hueco` redactada como "utilidad" (riesgo R-06)
  - Enrutamiento de respuestas de botón por prefijo de payload (`CONF:` → turnos, `OFER:` → ofertas) para que C-16 y C-37 registren sus handlers
  - Tests: firma inválida (401), webhook duplicado, adapter mockeado con WireMock
- **Dependencias**: `C-14`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/11_integracion_whatsapp.md` (completo)
  - `knowledge-base/07_flujos_principales.md` Flujo 2
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad (webhooks)
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-04, SU-03
  - `knowledge-base/10_preguntas_abiertas.md` PA-04

### [C-16] `recordatorios-y-confirmacion-whatsapp`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Job cada 5 min: selecciona turnos `RESERVADO` dentro de `recordatorio_horas_antes` sin `RECORDATORIO`, inserta `notificacion` `PENDIENTE` y envía la plantilla con botones "Confirmo"/"Cancelar" (RN-NO-01); payload `CONF:<token corto>` que resuelve el turno
  - Turnos creados con menos de 48 h: envío inmediato salvo que falten menos de 2 h (RN-NO-02)
  - Procesamiento de la respuesta del botón: "Confirmo" → `CONFIRMADO`; "Cancelar" → `CANCELADO` si se respeta `cancelacion_minima_horas`, si no avisa al consultorio y responde con el teléfono (RN-NO-06); acuse al paciente dentro de la ventana de 24 h; actualización del turno en menos de 1 min
  - Respaldo por email si el paciente no tiene WhatsApp, no dio consentimiento o falla el envío (RN-NO-03)
  - Auditoría de cada transición originada por el paciente (actor `PACIENTE`)
  - Hipótesis a medir: los recordatorios reducen ausencias (SU-03); dejar métricas de envío/confirmación listas para C-34
  - Tests: ventana de selección, idempotencia, cancelación fuera de plazo, fallback a email, webhook duplicado
- **Dependencias**: `C-14, C-15`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Notificaciones y confirmación (RN-NO-01 a RN-NO-08), RN-TU-02
  - `knowledge-base/07_flujos_principales.md` Flujo 2
  - `knowledge-base/11_integracion_whatsapp.md` §Plantillas, §Ventana de servicio de 24 h, §Errores y degradación
  - `knowledge-base/06_funcionalidades.md` US-030

### [C-17] `turnos-sin-confirmar-bandeja`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Job cada 15 min: turnos `RESERVADO` con `inicio ≤ ahora + confirmacion_limite_horas` y `sin_confirmar_alertado=false` → marca `true` y los agrega a la bandeja; **no cancela ni libera** (RN-NO-05)
  - `GET /api/v1/turnos/sin-confirmar` con paciente, teléfono, turno, ausencias previas y estado del recordatorio (enviado/entregado/leído)
  - Acciones: marcar "contactado", cancelar (dispara el hueco libre), enlace click-to-chat `wa.me`
  - Resumen por email al consultorio y aviso in-app; si el paciente confirma después de la alerta el turno sale de la bandeja
  - Tests: ventana del job, no re-alerta, salida de la bandeja al confirmar
- **Dependencias**: `C-16`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-031
  - `knowledge-base/05_reglas_de_negocio.md` RN-NO-05
  - `knowledge-base/07_flujos_principales.md` Flujo 3
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-06

### [C-18] `lista-de-espera-manual`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Migración: tabla `lista_espera` (`paciente_id`, `prestacion_id NULL`, `preferencia_texto VARCHAR(200)`, `estado ACTIVA/ASIGNADA/DESCARTADA`, `creado_at`)
  - CRUD `/api/v1/lista-espera`; `GET /lista-espera/sugeridas?prestacion` filtrada por prestación y ordenada por antigüedad (RN-LE-01)
  - `POST /lista-espera/{id}/asignar`: crea el turno (reutiliza el alta interna de C-10) y marca `ASIGNADA` (RN-LE-02); la oferta por WhatsApp va en C-37
  - Tests: orden por antigüedad, asignación transaccional (si el turno falla la entrada sigue `ACTIVA`)
- **Dependencias**: `C-10`
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-032
  - `knowledge-base/05_reglas_de_negocio.md` RN-LE-01, RN-LE-02, RN-LE-03
  - `knowledge-base/07_flujos_principales.md` Flujo 4
  - `knowledge-base/04_modelo_de_datos.md` §lista_espera

### [C-19] `frontend-bandeja-y-lista-de-espera`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: bandeja "Sin confirmar", panel de hueco liberado con sugeridos y selección para ofertar, panel de oferta abierta, lista de espera, a 390x844 y 1440x900):
  - Bandeja de turnos sin confirmar con estado del recordatorio, ausencias previas, botones click-to-chat, cancelar y marcar "contactado" (US-031)
  - Lista de espera: alta, listado, descartar; al cancelarse un turno la agenda muestra el hueco y el panel de sugeridos con "Asignar" y **selección de 1 a 5 pacientes + "Ofrecer por WhatsApp"** (US-032, Flujo 4)
  - Panel de oferta abierta: destinatarios con estado (enviada, leída, fallida), cuenta regresiva hasta el vencimiento, botón cancelar; actualización automática cuando alguien la toma (polling corto o SSE)
  - Aviso in-app de nuevos turnos sin confirmar en la navegación
  - Tests de componentes y del flujo cancelar → hueco → asignar
- **Dependencias**: `C-13, C-17, C-18, C-37`
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-031, US-032
  - `knowledge-base/07_flujos_principales.md` Flujo 3, Flujo 4
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura (frontend)
  - `design-system/README.md`

---

### [C-37] `oferta-de-huecos-por-whatsapp`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Migración: tablas `oferta_hueco` (con exclusion constraint de una sola oferta `ABIERTA` por rango) y `oferta_destinatario`; `consultorio.oferta_vencimiento_minutos`; `notificacion.turno_id` pasa a nullable + `oferta_destinatario_id`, CHECK de exclusividad e índices únicos parciales (ver `04_modelo_de_datos.md`)
  - `POST /api/v1/ofertas` (hueco + 1 a 5 entradas de lista de espera), `GET /ofertas/{id}`, `POST /ofertas/{id}/cancelar`; validaciones RN-LE-04 (consentimiento de WhatsApp, hueco a más de 2 h) y RN-LE-07
  - Envío de la plantilla `oferta_hueco` por el outbox (C-14) a cada destinatario, con payload `OFER:<token_respuesta>`; sin respaldo por email
  - Handler de respuesta "Lo tomo" (C-15): transacción que valida la oferta `ABIERTA` y no vencida, crea el turno `CONFIRMADO` (la exclusion constraint de `turno` define al ganador), marca oferta `TOMADA`, destinatario `ACEPTADA` y entrada `ASIGNADA`; respuestas de servicio al ganador y a los que llegan tarde (`TARDE`)
  - Job cada minuto: ofertas vencidas → `VENCIDA`; si el hueco se ocupó por otra vía → `CANCELADA`
  - La disponibilidad pública (C-11/C-20) descuenta los rangos con oferta `ABIERTA` (RN-LE-07)
  - Auditoría: creación, cancelación, aceptación y vencimiento de ofertas
  - Tests (Testcontainers): dos aceptaciones simultáneas → un solo turno; aceptación tras vencimiento; segunda oferta sobre el mismo rango rechazada; hueco tomado por otra vía; idempotencia del webhook
- **Dependencias**: `C-15, C-18`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-10, R-06
  - `knowledge-base/05_reglas_de_negocio.md` RN-LE-01 a RN-LE-07, RN-AG-01, RN-NO-07
  - `knowledge-base/07_flujos_principales.md` Flujo 4
  - `knowledge-base/04_modelo_de_datos.md` §oferta_hueco, §oferta_destinatario, §notificacion
  - `knowledge-base/11_integracion_whatsapp.md` §Plantillas (`oferta_hueco`)
  - `knowledge-base/06_funcionalidades.md` US-032, US-033

---

## FASE 4 — Reserva online del paciente

### [C-20] `reserva-online-api-publica`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - `GET /api/v1/public/disponibilidad?prestacion&desde` (usa C-11) y `GET /public/prestaciones` y horarios del consultorio (solo reservables)
  - `POST /api/v1/public/reservas`: en una transacción busca o crea el paciente por DNI (sin sobrescribir datos clínicos), valida RN-AG-06 (máx. 2 turnos online futuros → 422), inserta turno `RESERVADO` con `origen=ONLINE` y `token_publico`; conflicto de la exclusion constraint → 409 "ese horario se acaba de ocupar"
  - Consentimiento obligatorio de datos y WhatsApp con fecha y versión de la política (RN-DP-01); publica `TurnoCreado` (confirmación por WhatsApp/email, auditoría con actor `PACIENTE`)
  - Rate limiting por IP, validación estricta de input, sin enumeración de pacientes por DNI
  - Decisión abierta PA-05 (DNI + celular vs. OTP): v1 sigue DD-05 (sin verificación); dejar el punto de extensión
  - Tests: dos reservas simultáneas del mismo horario, límite de 2 turnos, DNI existente, rate limit
- **Dependencias**: `C-09, C-11, C-14`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-020
  - `knowledge-base/07_flujos_principales.md` Flujo 1
  - `knowledge-base/05_reglas_de_negocio.md` RN-AG-04, RN-AG-05, RN-AG-06, RN-DP-01, RN-GL-03
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-05
  - `knowledge-base/10_preguntas_abiertas.md` PA-05

### [C-21] `gestion-turno-por-enlace`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - `GET /api/v1/public/turnos/{token}`, `POST .../cancelar`, `POST .../reprogramar`, autorizadas por `token_publico` (256 bits, de un solo propósito, vence al terminar el turno, RN-GL-03)
  - Cancelar/reprogramar permitido hasta `cancelacion_minima_horas`; después responde con el teléfono del consultorio (RN-TU-02)
  - Reprogramar = cancelar el original y crear uno nuevo vinculado (`reprogramado_de`), sin dos turnos activos (RN-TU-03)
  - Al cancelar, el hueco queda disponible para la lista de espera sugerida (RN-LE-01)
  - Notificaciones de cancelación/reprogramación; auditoría con actor `PACIENTE`
  - Tests: token vencido/inválido, fuera de plazo, reprogramación atómica, rate limit
- **Dependencias**: `C-20`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-021
  - `knowledge-base/05_reglas_de_negocio.md` RN-TU-02, RN-TU-03, RN-GL-03
  - `knowledge-base/03_actores_y_roles.md` §RBAC (Paciente), §Rutas públicas
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad

### [C-22] `frontend-reserva-publica`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: reserva paso a paso, confirmación, gestión de turno por enlace, páginas legales; mobile-first a 390x844 y 1440x900):
  - `features/reserva-publica`: elegir prestación → día/horario libre → datos (DNI, nombre, apellido, celular, email) con casillas de política de privacidad y consentimiento WhatsApp → confirmación con enlace del turno
  - Mensaje "ese horario se acaba de ocupar" con recarga de disponibilidad (409); mensaje de 422 por límite de turnos
  - Página `/turno/{token}` para ver, cancelar y reprogramar; fuera de plazo muestra el teléfono
  - Páginas `/privacidad` y `/terminos` (obligatorias por Ley 25.326) con listado de subencargados; rutas `/` y `/reservar`
  - Accesibilidad, rendimiento mobile y tests de flujos
- **Dependencias**: `C-07, C-20, C-21`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-020, US-021
  - `knowledge-base/07_flujos_principales.md` Flujo 1
  - `knowledge-base/12_cumplimiento_legal.md` §Roles legales, §Entregables legales
  - `knowledge-base/03_actores_y_roles.md` §Rutas públicas
  - `design-system/README.md`

---

## FASE 5 — Historia clínica

> Rama paralela a agenda/notificaciones: arranca apenas termina C-09. Todo el módulo es de datos sensibles de salud (Ley 26.529 / 25.326).

### [C-23] `historia-clinica-append-only`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Migración: tabla `entrada_historia` (`tipo ANAMNESIS/EVOLUCION/ODONTOGRAMA/ENMIENDA`, `contenido JSONB`, `autor_id`, `turno_id NULL`, `enmienda_de NULL`, `hash_sha256`, `hash_anterior`)
  - Inalterabilidad: trigger que rechaza UPDATE/DELETE y permisos del usuario de base de la app sin UPDATE/DELETE (RN-HC-01); hash encadenado por paciente (RN-HC-02) con verificación de integridad
  - Endpoints solo odontólogo: `POST /api/v1/pacientes/{id}/historia/anamnesis`, `/evolucion`, `/enmiendas`; `GET` de la historia (cada lectura audita `VER_HISTORIA`, RN-SEG-05); la secretaria recibe 403 (RN-SEG-02)
  - Autor con nombre y matrícula en cada entrada (RN-HC-06); anamnesis estructurada (antecedentes, alergias, medicación) con versiones; las anteriores quedan como historial
  - Decisión abierta PA-12 (firma digital Ley 25.506): v1 usa usuario autenticado + hash
  - Tests: UPDATE/DELETE rechazados en base, cadena de hashes, enmienda referencia a la original, 403 a secretaria, auditoría de lectura
- **Dependencias**: `C-03, C-09`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §entrada_historia
  - `knowledge-base/05_reglas_de_negocio.md` §Historia clínica (RN-HC-01 a RN-HC-06), RN-SEG-02, RN-SEG-05
  - `knowledge-base/06_funcionalidades.md` US-041, US-043
  - `knowledge-base/12_cumplimiento_legal.md` (completo)
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-07

### [C-24] `odontograma-backend`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Migración: tabla `odontograma_pieza` (`entrada_id`, `pieza` FDI 11-48 y 51-85, `cara O/M/D/V/L/P/I/COMPLETA`, `estado`, `realizado`)
  - Catálogo de estados del odontograma como enum en código (caries, obturado, ausente, corona, endodoncia, a extraer, …); convención de colores a definir con el odontólogo asesor (PA-08)
  - `POST /pacientes/{id}/historia/odontograma` crea una entrada `ODONTOGRAMA` con todas sus piezas (nueva versión); `GET` del vigente y de la evolución en el tiempo (US-042)
  - Validación de numeración FDI (permanente y temporal) y de combinaciones pieza/cara
  - Misma inalterabilidad y hash encadenado que C-23
  - Tests: validación FDI, versionado, distinción realizado/a realizar
- **Dependencias**: `C-23`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-042
  - `knowledge-base/04_modelo_de_datos.md` §odontograma_pieza
  - `knowledge-base/05_reglas_de_negocio.md` RN-HC-01, RN-HC-02
  - `knowledge-base/10_preguntas_abiertas.md` PA-08

### [C-25] `exportacion-pdf-historia-clinica`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - `GET /api/v1/pacientes/{id}/historia/pdf`: PDF completo de la historia (anamnesis, evoluciones, enmiendas con su autor/matrícula/fecha, odontograma vigente y evolución), solo odontólogo
  - Cada generación audita `EXPORTAR` (o acción equivalente de copia entregada)
  - Cumple RN-HC-05: poder entregar la copia dentro de las 48 h de solicitada; generación en menos de unos segundos para historias típicas
  - Tests: contenido del PDF con enmiendas, auditoría, permisos
- **Dependencias**: `C-23, C-24`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-044
  - `knowledge-base/05_reglas_de_negocio.md` RN-HC-05, RN-HC-06
  - `knowledge-base/07_flujos_principales.md` Flujo 7
  - `knowledge-base/12_cumplimiento_legal.md` §Cómo responde el diseño (derecho a copia)

### [C-26] `frontend-ficha-paciente-y-evolucion`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: ficha del paciente, listado/búsqueda de pacientes, anamnesis, evolución y enmiendas, a 390x844 y 1440x900):
  - `features/pacientes`: listado y búsqueda; ficha con datos, contador de ausencias, turnos y sección de historia clínica (US-040); el bloque de saldo se completa en C-29
  - Anamnesis con formulario estructurado, alergias y medicación destacadas, historial de versiones (US-041)
  - Evolución clínica por consulta y enmiendas (nunca edición) (US-043); marca el turno `ATENDIDO` desde el flujo de atención (Flujo 5)
  - La secretaria no ve la sección clínica; edición de datos personales para ambos roles
  - Tests de componentes y de permisos de UI
- **Dependencias**: `C-07, C-09, C-10, C-23`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-040, US-041, US-043
  - `knowledge-base/07_flujos_principales.md` Flujo 5
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `design-system/README.md`

### [C-27] `frontend-odontograma-y-copia-hc`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: odontograma interactivo con dentición permanente/temporal, líneas de tiempo de versiones, descarga de copia, a 390x844 y 1440x900):
  - Componente de odontograma FDI: selección de pieza y cara, estados, "a realizar" vs. "realizado", guardado como nueva versión (US-042)
  - Visor de versiones anteriores (evolución en el tiempo)
  - Botón "Entregar copia en PDF" (consume C-25)
  - Accesibilidad (navegación por teclado, no depender solo del color) y tests de interacción
- **Dependencias**: `C-24, C-25, C-26`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-042, US-044
  - `knowledge-base/04_modelo_de_datos.md` §odontograma_pieza
  - `knowledge-base/10_preguntas_abiertas.md` PA-08
  - `design-system/README.md`

---

## FASE 6 — Caja

### [C-28] `caja-presupuestos-y-cobros`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Migración: `presupuesto` (`estado BORRADOR/ENTREGADO/ACEPTADO/RECHAZADO`, `total`, `validez_dias`), `presupuesto_item` (`prestacion_id`, `pieza NULL`, `cantidad`, `precio_unitario`), `cobro` (`monto > 0`, `medio`, `anulado`, `anulado_motivo`, `registrado_por`)
  - Endpoints: CRUD de presupuestos (odontólogo; secretaria solo lectura), `GET /presupuestos/{id}/pdf`, `POST /cobros`, `POST /cobros/{id}/anular` (solo odontólogo, con motivo, RN-CA-01), `GET /pacientes/{id}/saldo`
  - Saldo = presupuestos `ACEPTADO` − cobros no anulados (RN-CA-02); montos en ARS con 2 decimales (RN-CA-04); sin comprobantes fiscales (RN-CA-03)
  - Auditoría de toda creación y anulación (RN-GL-01)
  - Tests: cálculo de saldo, anulación solo por odontólogo, cobros nunca se borran
- **Dependencias**: `C-03, C-05, C-09`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-050, US-051
  - `knowledge-base/04_modelo_de_datos.md` §presupuesto / presupuesto_item, §cobro
  - `knowledge-base/05_reglas_de_negocio.md` §Caja (RN-CA-01 a RN-CA-04)
  - `knowledge-base/03_actores_y_roles.md` §RBAC (Presupuestos, Cobros)
  - `knowledge-base/09_decisiones_y_supuestos.md` SU-04

### [C-29] `frontend-caja`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: armado de presupuesto, registro de cobro, saldo en la ficha, a 390x844 y 1440x900):
  - Armado de presupuesto con prestaciones y piezas, estados y descarga del PDF (US-050)
  - Registro de cobros por medio de pago y anulación (solo odontólogo, con motivo); saldo en la ficha del paciente (US-051)
  - Permisos de UI por rol (la secretaria registra cobros pero no anula ni edita presupuestos)
  - Tests de componentes
- **Dependencias**: `C-26, C-28`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-050, US-051
  - `knowledge-base/05_reglas_de_negocio.md` RN-CA-01 a RN-CA-04
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `design-system/README.md`

### [C-30] `resumen-de-caja` (S)
- **Estado**: `[-]` **postergado post-MVP** (DD-13, PA-01)
- **Scope** (**Requiere mockup HTML aprobado**: resumen de caja):
  - `GET /api/v1/caja/resumen?desde&hasta&agrupar=dia|semana|mes` discriminado por medio de pago (cobros no anulados)
  - Pantalla de resumen con filtros de período (US-052); solo odontólogo
  - Tests: agregaciones por período y medio, exclusión de anulados
- **Dependencias**: `C-07, C-28`
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-052
  - `knowledge-base/04_modelo_de_datos.md` §cobro
  - `knowledge-base/05_reglas_de_negocio.md` RN-CA-01, RN-CA-04

---

## FASE 7 — Integraciones, auditoría y datos

### [C-31] `google-calendar-sync` (S)
- **Estado**: `[-]` **postergado post-MVP** (DD-13, PA-01)
- **Scope** (**Requiere mockup HTML aprobado**: pantalla de conexión de Google Calendar en configuración):
  - Migración: tabla `integracion_google` (`refresh_token_cifrado BYTEA` con AES-GCM y `CRYPTO_KEY`, `calendar_id`)
  - OAuth2 (`GOOGLE_CLIENT_ID/SECRET`), `CalendarioPort` → `GoogleCalendarAdapter`; sincronización unidireccional (DD-09) por listeners de `TurnoCreado/Reprogramado/Cancelado`, con `google_event_id`
  - El evento lleva solo nombre de pila, inicial del apellido y prestación, sin datos clínicos (minimización); un fallo de Google no bloquea la agenda y se reintenta
  - UI mínima en configuración: conectar/desconectar y elegir calendario
  - Tests: adapter mockeado, reintentos, token cifrado, contenido del evento
- **Dependencias**: `C-08, C-10`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-014
  - `knowledge-base/04_modelo_de_datos.md` §integracion_google
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-09
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad (datos sensibles)
  - `knowledge-base/12_cumplimiento_legal.md` §Cómo responde el diseño (minimización)

### [C-32] `consulta-de-auditoria`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: pantalla de auditoría con filtros):
  - `GET /api/v1/auditoria?actor&accion&entidad&desde&hasta` paginado, solo odontólogo (secretaria sin acceso)
  - Pantalla con filtros por actor, acción, entidad y fecha (quién vio o modificó qué y cuándo, US-060)
  - Verificación de que las acciones clave de los módulos existentes estén registradas (`VER_HISTORIA`, `CREAR_TURNO`, `EXPORTAR`, cobros, usuarios)
  - Tests: filtros, paginación, permisos, sin edición ni borrado
- **Dependencias**: `C-02, C-07`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-060
  - `knowledge-base/05_reglas_de_negocio.md` RN-GL-01, RN-SEG-05
  - `knowledge-base/04_modelo_de_datos.md` §evento_auditoria
  - `knowledge-base/03_actores_y_roles.md` §RBAC (Auditoría)

### [C-33] `exportacion-total-de-datos`
- **Estado**: `[ ]` pendiente
- **Scope** (**Requiere mockup HTML aprobado**: pantalla mínima de exportación en configuración):
  - `POST /api/v1/exportaciones`: job asíncrono que genera un ZIP con CSV/JSON por entidad y PDFs de las historias clínicas, cifrado con la contraseña que elige el odontólogo (RN-DP-03)
  - `GET /exportaciones/{id}` y descarga con enlace que vence a las 24 h; auditoría `EXPORTAR`
  - Incluye pacientes, turnos, historia clínica, odontogramas, presupuestos, cobros, notificaciones y auditoría; solo odontólogo
  - Tests: contenido completo, cifrado del ZIP, vencimiento del enlace, permisos
- **Dependencias**: `C-25, C-28`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-061
  - `knowledge-base/07_flujos_principales.md` Flujo 7
  - `knowledge-base/05_reglas_de_negocio.md` RN-DP-03
  - `knowledge-base/12_cumplimiento_legal.md` §Cómo responde el diseño (portabilidad)

### [C-34] `indicadores-de-ausentismo` (S)
- **Estado**: `[-]` **postergado post-MVP** (DD-13, PA-01)
- **Scope** (**Requiere mockup HTML aprobado**: tablero de indicadores):
  - `GET /api/v1/indicadores/ausentismo?mes=` con tasa de ausencias, confirmaciones, reservas online y **huecos recuperados** (directo u oferta, US-062), solo odontólogo
  - Tablero simple con comparación contra el mes anterior; útil para validar la hipótesis de SU-03
  - Tests: cálculo con turnos de distintos orígenes y estados
- **Dependencias**: `C-07, C-16, C-20`
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` US-062
  - `knowledge-base/01_vision_y_objetivos.md`
  - `knowledge-base/09_decisiones_y_supuestos.md` SU-03

---

## FASE 8 — Operación y lanzamiento

### [C-35] `despliegue-por-instancia-y-operacion`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Imagen versionada única (backend con SPA embebida); infraestructura compartida (DD-01 revisada): host o plataforma de contenedores con reverse proxy TLS y subdominio por consultorio, y un clúster PostgreSQL gestionado con una base y un usuario por consultorio (`REVOKE CONNECT ... FROM PUBLIC`)
  - Script de alta de un consultorio: base + usuario + subdominio + variables de entorno + contenedor + asistente de instalación
  - Límites de CPU y memoria por contenedor y pool de conexiones acotado por instancia (vecino ruidoso, R-01)
  - Pipeline/script que actualiza todas las instancias con rollout gradual (primero una canaria) y corre Flyway en cada base, con rollback documentado; asistente de instalación (seed del consultorio y del odontólogo, `ADMIN_INITIAL_EMAIL`)
  - Backups automáticos diarios del clúster con retención ≥ 30 días, más `pg_dump` diario por base para restaurar un solo consultorio; procedimiento de restauración probado (trimestral)
  - Logs estructurados y alertas mínimas (token de Meta vencido, fallo de jobs, base caída); gestión de secretos por variables de entorno
  - **Alcance para el MVP/demo (DD-11)**: un VPS económico con `docker-compose` (Caddy con TLS, contenedores de la app, PostgreSQL en contenedor con una base por consultorio, `pg_dump` diario a almacenamiento externo); solo datos ficticios (SU-07). El clúster gestionado y la alta disponibilidad se implementan al pasar a producción (PA-13, PA-09)
  - IN-01 resuelta: automatizar el despliegue y el alta de consultorios desde el día 1
  - Anticipable: solo requiere C-01; conviene tener un ambiente de staging temprano
  - Tests: alta de un consultorio desde cero, verificar que un usuario de base no puede conectarse a otra base, migrar todas las instancias de una versión a otra, restaurar un solo consultorio desde su `pg_dump`
- **Dependencias**: `C-01`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/08_arquitectura_propuesta.md` §Despliegue por instancia, §Variables de entorno, §Seguridad (respaldo, secretos)
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-01, DD-03, riesgo R-01
  - `knowledge-base/10_preguntas_abiertas.md` IN-01, PA-03, PA-09

### [C-36] `hardening-seguridad-y-cumplimiento-legal`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Cabeceras de seguridad (CSP, HSTS, `X-Content-Type-Options`, `Referrer-Policy`), revisión de rate limiting, CSRF y CORS en todos los endpoints públicos
  - Auditoría de permisos de base: confirmar que el usuario de la app no tiene UPDATE/DELETE sobre `entrada_historia` ni `evento_auditoria`; verificación de cifrado en reposo y TLS
  - Barrido de la matriz RBAC completa (tests de integración por rol) y de que ningún log contiene datos clínicos
  - Checklist de Resolución AAIP 47/2018; pruebas end-to-end de los flujos 1 a 7 en staging
  - Entregables legales para el lanzamiento (documentos, no código): política de privacidad, términos, DPA, listado de subencargados, texto de consentimiento, procedimientos de derechos ARCO e incidentes, guía de inscripción en la AAIP; revisión del asesor legal (PA-09, PA-12)
  - Prueba de restauración de backup y plan de rollback del primer despliegue productivo
- **Dependencias**: `C-12, C-16, C-19, C-22, C-27, C-29, C-32, C-33, C-35`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/12_cumplimiento_legal.md` (completo)
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `knowledge-base/09_decisiones_y_supuestos.md` §Riesgos
  - `knowledge-base/10_preguntas_abiertas.md` PA-09, PA-12

---

## Tabla resumen

| ID | Nombre | Fase | US cubiertas | Gov. | Deps | UI/mockup |
|----|--------|------|--------------|------|------|-----------|
| C-01 | scaffolding-monorepo-y-base-tecnica | 0 | — | BAJO | — | no |
| C-02 | auditoria-append-only | 1 | (US-060 base) | CRITICO | C-01 | no |
| C-03 | identidad-auth-y-rbac | 1 | US-003 (parcial) | CRITICO | C-01, C-02 | no |
| C-04 | email-adapter-y-recuperacion-password | 1 | US-003 | ALTO | C-03 | no |
| C-05 | configuracion-consultorio-y-prestaciones | 1 | US-001 | MEDIO | C-03 | no |
| C-06 | gestion-usuarios-internos | 1 | US-002 | ALTO | C-03 | no |
| C-07 | frontend-shell-login-y-layout | 1 | — | BAJO | C-03 | sí |
| C-08 | frontend-configuracion-usuarios-y-recuperacion | 1 | US-001, 002, 003 | BAJO | C-04..C-07 | sí |
| C-09 | pacientes-core | 2 | (US-011 parcial) | ALTO | C-02, C-03 | no |
| C-10 | agenda-turnos-core | 2 | US-011 | CRITICO | C-05, C-09 | no |
| C-11 | bloqueos-y-disponibilidad | 2 | US-012 | MEDIO | C-10 | no |
| C-12 | asistencia-y-ausencias | 2 | US-013 | MEDIO | C-10 | no |
| C-13 | frontend-agenda | 2 | US-010..013 | MEDIO | C-07, C-10..C-12 | sí |
| C-14 | notificaciones-outbox-y-email | 3 | — | ALTO | C-04, C-10 | no |
| C-15 | whatsapp-adapter-y-webhooks | 3 | US-030 (parcial) | ALTO | C-14 | no |
| C-16 | recordatorios-y-confirmacion-whatsapp | 3 | US-030 | ALTO | C-14, C-15 | no |
| C-17 | turnos-sin-confirmar-bandeja | 3 | US-031 | MEDIO | C-16 | no |
| C-18 | lista-de-espera-manual | 3 | US-032 | BAJO | C-10 | no |
| C-19 | frontend-bandeja-y-lista-de-espera | 3 | US-031, 032 | BAJO | C-13, C-17, C-18, C-37 | sí |
| C-20 | reserva-online-api-publica | 4 | US-020 | ALTO | C-09, C-11, C-14 | no |
| C-21 | gestion-turno-por-enlace | 4 | US-021 | ALTO | C-20 | no |
| C-22 | frontend-reserva-publica | 4 | US-020, 021 | MEDIO | C-07, C-20, C-21 | sí |
| C-23 | historia-clinica-append-only | 5 | US-041, 043 | CRITICO | C-03, C-09 | no |
| C-24 | odontograma-backend | 5 | US-042 | ALTO | C-23 | no |
| C-25 | exportacion-pdf-historia-clinica | 5 | US-044 | ALTO | C-23, C-24 | no |
| C-26 | frontend-ficha-paciente-y-evolucion | 5 | US-040, 041, 043 | MEDIO | C-07, C-09, C-10, C-23 | sí |
| C-27 | frontend-odontograma-y-copia-hc | 5 | US-042, 044 | MEDIO | C-24, C-25, C-26 | sí |
| C-28 | caja-presupuestos-y-cobros | 6 | US-050, 051 | ALTO | C-03, C-05, C-09 | no |
| C-29 | frontend-caja | 6 | US-050, 051 | MEDIO | C-26, C-28 | sí |
| C-30 | resumen-de-caja (S) — postergado | 6 | US-052 | BAJO | C-07, C-28 | sí |
| C-31 | google-calendar-sync (S) — postergado | 7 | US-014 | ALTO | C-08, C-10 | sí |
| C-32 | consulta-de-auditoria | 7 | US-060 | MEDIO | C-02, C-07 | sí |
| C-33 | exportacion-total-de-datos | 7 | US-061 | ALTO | C-25, C-28 | sí |
| C-34 | indicadores-de-ausentismo (S) — postergado | 7 | US-062 | BAJO | C-07, C-16, C-20 | sí |
| C-35 | despliegue-por-instancia-y-operacion | 8 | — | ALTO | C-01 | no |
| C-36 | hardening-seguridad-y-cumplimiento-legal | 8 | — | CRITICO | C-12, C-16, C-19, C-22, C-27, C-29, C-32, C-33, C-35 | no |
| C-37 | oferta-de-huecos-por-whatsapp | 3 | US-032, 033 | ALTO | C-15, C-18 | no |

Primer item elegible: **C-01** (`scaffolding-monorepo-y-base-tecnica`).
