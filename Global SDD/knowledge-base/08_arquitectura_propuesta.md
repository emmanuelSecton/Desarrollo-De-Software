# Arquitectura Propuesta

## Patrones aplicados

| Patrón | Dónde se usa | Por qué |
|---|---|---|
| Monolito modular (package-by-feature) | Backend: un paquete por dominio (`agenda`, `pacientes`, `clinica`, `caja`, `notificaciones`, `identidad`, `auditoria`, `integraciones`) | Entrega rápida y un solo despliegue por instancia. Los límites entre módulos permiten separarlos después. Se pueden verificar con Spring Modulith o ArchUnit. |
| Arquitectura en capas por módulo (api → application → domain → infrastructure) | Dentro de cada módulo | Separa reglas de negocio de frameworks y facilita testear la lógica sin base de datos. |
| Eventos de dominio in-process | `TurnoCancelado`, `TurnoCreado` → notificaciones, Google, auditoría | Desacopla los módulos; la agenda no conoce WhatsApp. Se publican con `@TransactionalEventListener(AFTER_COMMIT)`. |
| Outbox simple (tabla `notificacion`) | Envíos de WhatsApp y email | Garantiza que no se pierdan ni dupliquen mensajes aunque falle el proveedor, y permite reintentos. |
| Append-only + hash encadenado | `entrada_historia`, `evento_auditoria` | Inalterabilidad e integridad de la historia clínica (Ley 26.529). |
| Adapter / puerto | `MensajeriaPort` → `WhatsAppCloudAdapter`, `EmailAdapter`; `CalendarioPort` → `GoogleCalendarAdapter` | Se puede cambiar de proveedor y testear con dobles. |
| Optimistic locking (`@Version`) + exclusion constraint | `turno` | Evita pisadas concurrentes y solapamientos. |
| Feature folders | Frontend | Cada feature (agenda, pacientes, reserva pública) tiene sus componentes, hooks y llamadas a la API. |

## Estructura de directorios

```
global-sdd/
├── backend/
│   ├── build.gradle (o pom.xml)
│   └── src/main/java/<paquete>/
│       ├── agenda/          (api, application, domain, infrastructure)
│       ├── pacientes/
│       ├── clinica/         (historia, odontograma, enmiendas)
│       ├── caja/
│       ├── notificaciones/  (outbox, jobs, plantillas)
│       ├── integraciones/   (whatsapp, google, email)
│       ├── identidad/       (usuarios, auth, seguridad)
│       ├── auditoria/
│       └── shared/          (errores, tiempo, utilidades)
│   └── src/main/resources/db/migration/   (Flyway V1__..., V2__...)
├── frontend/
│   └── src/
│       ├── features/   (agenda, pacientes, clinica, caja, reserva-publica, configuracion)
│       ├── shared/     (ui, api-client, auth, formatos)
│       └── pages/      (rutas)
├── deploy/
│   ├── Dockerfile
│   └── docker-compose.yml   (dev: app + postgres)
└── knowledge-base/
```

**Suposición:** backend y frontend viven en el mismo repositorio (monorepo) y se despliegan en una sola imagen, con el build de la SPA servido como recursos estáticos por Spring Boot. Es más barato de operar por instancia.

## Seguridad

- **Autenticación**: sesión de servidor con Spring Security (cookie `HttpOnly`, `Secure`, `SameSite=Strict`) y protección CSRF; contraseñas con BCrypt (cost ≥ 12). Una SPA servida desde el mismo origen no necesita JWT.
- **Autorización**: RBAC por rol (`ODONTOLOGO`, `SECRETARIA`) con `@PreAuthorize`. El paciente se autoriza por `token_publico` del turno (256 bits, de un solo propósito, vence al terminar el turno).
- **Validación de input**: Bean Validation en los DTO de entrada y validación espejo en el frontend; nunca se confía solo en el cliente.
- **Webhooks**: verificación de la firma `X-Hub-Signature-256` con el App Secret de Meta y comparación en tiempo constante.
- **Datos sensibles**: TLS en tránsito; cifrado en reposo provisto por la base gestionada; el `refresh_token` de Google se cifra a nivel de aplicación (AES-GCM).
- **Mínimo privilegio en base**: el usuario de la app no tiene `UPDATE`/`DELETE` sobre `entrada_historia` ni sobre `evento_auditoria`.
- **Rate limiting**: endpoints públicos (`/public/**`, `/auth/login`) limitados por IP (por ejemplo Bucket4j).
- **Secrets management**: variables de entorno inyectadas por la plataforma de hosting; nunca en el repositorio. `.env.example` sin valores reales.
- **Respaldo**: backups automáticos diarios de la base gestionada con retención mínima de 30 días, más restauración probada trimestralmente.
- **Cabeceras**: CSP, HSTS, `X-Content-Type-Options`, `Referrer-Policy`.

## Variables de entorno

| Variable | Descripción | Ejemplo | Sensible |
|---|---|---|---|
| `DB_URL` | JDBC de PostgreSQL | `jdbc:postgresql://host:5432/consultorio` | N |
| `DB_USER` / `DB_PASSWORD` | Credenciales de la app | — | Y |
| `APP_BASE_URL` | URL pública de la instancia | `https://drperez.ejemplo.com.ar` | N |
| `APP_TIMEZONE` | Zona horaria por defecto | `America/Argentina/Buenos_Aires` | N |
| `WA_VERIFY_TOKEN` | Verificación inicial del webhook | — | Y |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | OAuth de Google Calendar (post-MVP, DD-13) | — | Y |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USER` / `SMTP_PASSWORD` | Envío de email | — | Y (password) |
| `MAIL_FROM` | Remitente | `turnos@ejemplo.com.ar` | N |
| `CRYPTO_KEY` | Clave AES para cifrar tokens: credenciales de WhatsApp de cada consultorio (DD-12) y Google | — | Y |

> Las credenciales de WhatsApp (`phone_number_id`, token de sistema y App Secret) **no** son variables de entorno: las carga cada odontólogo en el panel y se guardan cifradas en la base (DD-12).
| `ADMIN_INITIAL_EMAIL` | Email del odontólogo en la instalación | — | N |

## Despliegue por instancia

Cada consultorio = 1 contenedor (backend con la SPA embebida) + 1 **base** dentro del clúster PostgreSQL compartido + su propio número de WhatsApp (DD-01).

```
 Internet ──► Reverse proxy (TLS, *.dominio) ──┬─► contenedor consultorio A ──┐
                                               ├─► contenedor consultorio B ──┼─► Clúster PostgreSQL gestionado
                                               └─► contenedor consultorio N ──┘   (db_a, db_b, … db_n; un usuario por base)
```

- **Aislamiento**: cada contenedor tiene sus propias variables de entorno y credenciales; su usuario de base solo tiene permisos sobre su base (`REVOKE CONNECT ... FROM PUBLIC` en cada base).
- **Recursos**: límites de CPU y memoria por contenedor; pool de conexiones acotado por instancia (por ejemplo Hikari `maximumPoolSize` ≤ 5) para no agotar las conexiones del clúster.
- **Backups**: el backup del clúster gestionado cubre todas las bases, y se suma un `pg_dump` diario por base para poder restaurar un solo consultorio.
- **Actualizaciones**: todas las instancias se despliegan desde la misma imagen versionada. Un script o pipeline recorre las instancias, corre Flyway en cada base y hace un rollout gradual (primero una instancia canaria).
- **Alta de un consultorio**: script que crea la base y el usuario, el subdominio, las variables de entorno y el contenedor, y corre el asistente de instalación.

### Entorno de demo (DD-11)

Un VPS económico con un único `docker-compose.yml`:
- **Caddy** como reverse proxy con TLS automático;
- uno o dos contenedores de la app, cada uno un consultorio de demo;
- **PostgreSQL en contenedor**, con una base por consultorio y volumen persistente;
- un job de `pg_dump` diario a almacenamiento externo.

**Solo datos ficticios** (SU-07). El mismo compose sirve para desarrollo local. Para pasar a producción se reemplaza el contenedor de PostgreSQL por uno gestionado (PA-13) sin cambiar la aplicación.
