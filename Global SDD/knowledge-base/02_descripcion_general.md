# Descripción General

## Stack tecnológico

| Capa | Tecnologías | Versión mínima |
|---|---|---|
| Backend | Java + Spring Boot (Web, Security, Data JPA, Validation) | Java 21 LTS, Spring Boot 3.x |
| Persistencia | PostgreSQL + Flyway (migraciones) | PostgreSQL 16 |
| Frontend | React + Vite + TypeScript | React 18, TypeScript 5 |
| Tareas programadas | Spring Scheduling (`@Scheduled`) dentro del monolito | — |
| Mensajería | WhatsApp Business Platform (Cloud API de Meta) | Graph API vigente |
| Email | SMTP transaccional (proveedor a definir) | — |
| Calendario | Google Calendar API (OAuth 2.0) — **post-MVP** (DD-13) | v3 |
| Empaquetado | Docker (imagen única backend que sirve el build del frontend) | — |
| Hosting | **Demo**: un VPS económico con Docker Compose (Caddy + contenedores de la app + PostgreSQL en contenedor) (DD-11). **Producción**: host compartido + PostgreSQL gestionado, con una base por consultorio (PA-13) | — |

**Suposición:** Java 21 y Spring Boot 3 por ser las versiones LTS vigentes; ajustar si la cátedra o el equipo usan otras.

## Arquitectura general

**Monolito modular** en Spring Boot más una **SPA** en React. Cada consultorio cliente corre **su propia instancia** (contenedor + base de datos propia), sin multi-tenancy. Las instancias comparten el host y el clúster PostgreSQL (DD-01).

```
                    ┌──────────────────────── Instancia de un consultorio ───────────────────────┐
 Paciente ──HTTPS──►│  SPA React (reserva pública)                                               │
 Odontólogo ───────►│  SPA React (panel interno)                                                 │
 Secretaria ───────►│        │ REST/JSON                                                         │
                    │        ▼                                                                   │
                    │  Spring Boot (monolito modular)                                            │
                    │   ├─ agenda        ├─ pacientes     ├─ clinica (HC, odontograma)           │
                    │   ├─ notificaciones├─ caja          ├─ identidad (usuarios, roles)          │
                    │   └─ auditoria     └─ integraciones (whatsapp, google, email)               │
                    │        │                                    │            ▲                 │
                    │        ▼                                    ▼            │ webhooks        │
                    │   PostgreSQL gestionado            Meta Cloud API / Google / SMTP          │
                    └────────────────────────────────────────────────────────────────────────────┘
```

**Por qué así**:
- El monolito modular entrega rápido y es barato de operar: un solo despliegue por cliente. Los módulos tienen límites claros, así que pueden separarse más adelante.
- La SPA en TypeScript cubre los dos frentes, el panel interno y la página pública de reservas, con un solo stack de frontend.
- La instancia por cliente simplifica el aislamiento de datos de salud. Compartir host y clúster mantiene bajo el costo marginal por cliente (ver DD-01 y R-01).

## Integraciones externas

| Servicio | Propósito | Tipo |
|---|---|---|
| WhatsApp Cloud API (Meta) | Enviar recordatorios con plantillas aprobadas y recibir las respuestas del paciente | REST saliente + webhook entrante |
| Google Calendar API | Reflejar los turnos en el calendario del odontólogo — **post-MVP** (DD-13) | REST con OAuth 2.0 (unidireccional sistema → Google) |
| SMTP / proveedor de email | Recordatorio de respaldo, confirmación de reserva y recuperación de contraseña | SMTP o REST del proveedor |

Detalle de WhatsApp en `11_integracion_whatsapp.md`.

## API REST (resumen)

Prefijo `/api/v1`. Las rutas `/public/**` no requieren sesión de usuario interno.

| Recurso | Endpoints principales |
|---|---|
| Autenticación | `POST /auth/login`, `POST /auth/logout`, `POST /auth/password/recover` |
| Usuarios | `GET/POST/PUT /usuarios` (solo odontólogo) |
| Configuración | `GET/PUT /consultorio` (datos, horarios de atención), `GET/POST/PUT /prestaciones` |
| Agenda | `GET /turnos?desde&hasta`, `POST /turnos`, `PUT /turnos/{id}`, `POST /turnos/{id}/cancelar`, `POST /turnos/{id}/asistencia`, `GET/POST/DELETE /bloqueos` |
| Sin confirmar | `GET /turnos/sin-confirmar` |
| Lista de espera | `GET/POST/PUT/DELETE /lista-espera`, `POST /lista-espera/{id}/asignar` |
| Ofertas de hueco | `POST /ofertas` (hueco + destinatarios), `GET /ofertas/{id}`, `POST /ofertas/{id}/cancelar` |
| Pacientes | `GET /pacientes?q=`, `POST /pacientes`, `GET/PUT /pacientes/{id}`, `GET /pacientes/{id}/export` |
| Historia clínica | `GET /pacientes/{id}/historia`, `POST /pacientes/{id}/anamnesis`, `POST /pacientes/{id}/evoluciones`, `POST /pacientes/{id}/odontograma`, `POST /historia/{entradaId}/enmiendas` |
| Caja | `GET/POST /presupuestos`, `GET/POST /cobros`, `GET /pacientes/{id}/saldo` |
| Auditoría | `GET /auditoria?entidad&desde&hasta` |
| Exportación | `POST /exportaciones` (exportación completa del consultorio) |
| Público (paciente) | `GET /public/disponibilidad?prestacion&desde`, `POST /public/reservas`, `GET /public/turnos/{token}`, `POST /public/turnos/{token}/cancelar`, `POST /public/turnos/{token}/reprogramar` |
| Webhooks | `GET/POST /webhooks/whatsapp` (verificación + eventos de Meta) |
| Google | `GET /integraciones/google/connect`, `GET /integraciones/google/callback` |
