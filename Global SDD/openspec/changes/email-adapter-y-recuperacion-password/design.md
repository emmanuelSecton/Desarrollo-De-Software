# Design — email-adapter-y-recuperacion-password (C-04)

## Context

- C-03 dejó en `identidad`: tabla `usuario`, `PoliticaPassword` (10 caracteres a 72 bytes), bloqueo por intentos en la entidad `Usuario`, sesión de servidor en memoria creada a mano en `AuthController`, `FiltroLimiteDePeticiones` (login y `/public/**`) y `/api/v1/auth/password/recover` ya listado como ruta pública.
- La auditoría de C-02 escribe en `BEFORE_COMMIT`; el catálogo `AccionAuditoria` es un enum extensible sin cambio de base.
- El módulo `notificaciones` existe vacío. Ningún módulo envía emails todavía.
- PA-10 (proveedor de email transaccional) sigue abierta.
- `APP_BASE_URL` es obligatoria y está en `AppProperties` (`shared`).

## Goals / Non-Goals

**Goals:**
- Que pedir una recuperación no revele qué emails tienen cuenta, ni por la respuesta ni por el tiempo.
- Que un enlace robado sirva poco: una hora, una vez, y solo el último pedido.
- Que el envío de email sea reutilizable por C-14, C-15 y C-20 sin atarse a un proveedor.

**Non-Goals:**
- Cola persistente de reintentos de email (C-14 la trae para notificaciones de turnos).
- Cerrar las demás sesiones cuando el usuario cambia su propia contraseña con sesión (C-03): queda como mejora para otro change.
- Plantillas configurables por el consultorio.

## Decisions

### D1 — `MensajeriaPort` en `notificaciones`, adaptador SMTP
- `notificaciones.application.MensajeriaPort`: `void enviarEmail(MensajeEmail mensaje)`. `MensajeEmail` es un record con `tipo` (por ejemplo `RECUPERACION_PASSWORD`, usado solo para logs), destinatario, asunto, texto y HTML; su `toString()` no imprime nada de eso.
- `notificaciones.infrastructure.EmailSmtpAdapter` usa `JavaMailSender` de `spring-boot-starter-mail`, con `spring.mail.*` mapeado a `SMTP_HOST`, `SMTP_PORT` (587), `SMTP_USER`, `SMTP_PASSWORD`, STARTTLS si el servidor lo ofrece, y timeouts de conexión y escritura de 10 s.
- *Alternativa descartada*: la API REST de un proveedor (SES, SendGrid). Mientras PA-10 esté abierta, SMTP es lo único que ofrecen todos.

### D2 — Reintentos y modo deshabilitado
- Hasta 3 intentos con espera de 1 s y 3 s entre ellos (`Thread.sleep` en el hilo de fondo de D5, nunca en el de la petición). Si los 3 fallan: un `LOG.error` con tipo de mensaje y clase de la excepción, sin relanzar.
- Con `SMTP_HOST` vacío se registra `EmailDeshabilitadoAdapter` en lugar del SMTP (`@ConditionalOnProperty`), que solo hace `LOG.warn` con el tipo de mensaje.

### D3 — Tabla `token_recuperacion`
`V5__recuperacion_password.sql`:

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `UUID PK DEFAULT gen_random_uuid()` | |
| `usuario_id` | `UUID NOT NULL REFERENCES usuario(id)` | índice |
| `token_hash` | `CHAR(64) NOT NULL UNIQUE` | SHA-256 en hexadecimal |
| `creado_at` | `TIMESTAMPTZ NOT NULL` | del `Clock` |
| `vence_at` | `TIMESTAMPTZ NOT NULL` | `creado_at + 1 h` |
| `usado_at` | `TIMESTAMPTZ` | uso o invalidación |

- `CHECK (vence_at > creado_at)`. Grants: `SELECT, INSERT, UPDATE` al rol runtime, sin `DELETE`.
- El token tiene 32 bytes de `SecureRandom` en Base64 URL-safe (43 caracteres). Solo viaja en el email; en la base va su SHA-256. SHA-256 alcanza porque el token es aleatorio de 256 bits: no hace falta un hash lento como BCrypt.
- *Alternativa descartada*: columnas en `usuario`. Un usuario puede tener varios tokens en el tiempo, y separar la tabla deja su historial auditable.

### D4 — Flujo del pedido (`recover`)
`ServicioDeRecuperacion.solicitar(email, ip)` (`identidad.application`, `@Transactional`):
1. Busca el usuario por email. Si no existe o está inactivo, termina sin hacer nada más.
2. Marca `usado_at = ahora` en sus tokens sin usar (invalidación), crea uno nuevo, audita `SOLICITAR_RECUPERACION` (actor `SISTEMA`, entidad `USUARIO`, id del usuario) y publica el evento interno `RecuperacionSolicitada` con el destinatario, el nombre y el enlace.

El controlador responde siempre `202` con `{"mensaje": "Si el email corresponde a una cuenta, vas a recibir un enlace para restablecer la contraseña."}`.

### D5 — Envío después del commit y en segundo plano
`identidad.infrastructure.EnvioDeRecuperacion` escucha `RecuperacionSolicitada` con `@TransactionalEventListener(AFTER_COMMIT)` y `@Async`, y llama a `MensajeriaPort`. Así:
- El tiempo de respuesta de `recover` no incluye el SMTP: mide lo mismo exista o no la cuenta.
- Si la transacción se revierte, no sale ningún email con un token que no existe.

`@EnableAsync` usa el `applicationTaskExecutor` de Spring Boot. Los emails pendientes en memoria se pierden si la app se reinicia en ese instante; el usuario puede volver a pedirlo.
- *Alternativa descartada*: envío síncrono, como decía el scope de `CHANGES.md`. Con SMTP de por medio, el tiempo de respuesta revelaría qué emails tienen cuenta.

### D6 — Restablecimiento (`reset`)
`ServicioDeRecuperacion.restablecer(token, nueva, ip)` (`@Transactional`):
1. Calcula el SHA-256 y busca el token con `FOR UPDATE`. Inexistente, vencido, usado o de un usuario inactivo da `TokenInvalidoException` (`400`, mismo mensaje en todos los casos).
2. Valida `nueva` con `PoliticaPassword`. Si falla responde `400 /problemas/password-invalida` y el token sigue sirviendo, porque la transacción se revierte.
3. Guarda el hash BCrypt, `debe_cambiar_password = false`, limpia bloqueo e intentos, marca el token como usado y audita `MODIFICAR_ENTIDAD` (actor `USUARIO`, `referencia: recuperacion-password`).
4. Después del commit cierra las sesiones del usuario (D7).

### D7 — Cerrar las sesiones abiertas con `SessionRegistry`
- `ConfiguracionSeguridad` suma `sessionManagement().maximumSessions(-1).sessionRegistry(registro)` y un `expiredSessionStrategy` que responde `401 /problemas/no-autenticado`. Para que el registro se entere de las sesiones destruidas y de los ids rotados se registra un `HttpSessionEventPublisher`.
- `AuthController` registra la sesión en el `SessionRegistry` al iniciarla, porque el login es manual (C-03, D4).
- El cierre es un puerto de `identidad.application` (`CierreDeSesiones`) implementado en `infrastructure` con `sessionRegistry.getAllSessions(usuarioId, false).forEach(SessionInformation::expireNow)`.

### D8 — Rutas públicas y límite por IP
- `RutasDeSeguridad` agrega `/auth/password/reset` a las rutas públicas y a las permitidas con cambio pendiente; las dos rutas siguen exigiendo CSRF.
- `FiltroLimiteDePeticiones` aplica el cupo de login (`RATE_LIMIT_LOGIN_POR_MINUTO`) a `recover` y `reset`, en un bucket por IP compartido con el login.

### D9 — Contenido del email
Asunto "Restablecé tu contraseña". El cuerpo saluda por nombre, explica que el enlace vence en 1 hora y que, si no fue el usuario, puede ignorar el mensaje. No incluye datos clínicos ni del consultorio más allá del nombre del producto (`brand` en `(TBD)`). El HTML es simple, con estilos en línea y sin imágenes remotas.

### D10 — Tests
- `EmailSmtpAdapterTest` con GreenMail (`greenmail-junit5`), un servidor SMTP real en memoria: envío correcto con texto y HTML, reintento tras un fallo y 3 fallos con un solo log de error.
- Los tests del flujo reemplazan `MensajeriaPort` por un doble `@Primary` que guarda los mensajes y espera el envío asíncrono.
- `RelojDePrueba` para el vencimiento a la hora.

## Risks / Trade-offs

- [Un email en memoria se pierde si la app se reinicia justo después del commit] → El usuario vuelve a pedirlo; C-14 traerá una cola persistente para notificaciones de turnos.
- [El token viaja en la URL] → `Referrer-Policy: strict-origin-when-cross-origin` (C-03) evita que salga en el `Referer`; vence a la hora y sirve una vez.
- [Sin proveedor elegido (PA-10), en la demo puede no haber SMTP] → El modo deshabilitado no rompe nada; en desarrollo, Mailpit muestra los emails.
- [Cambiar la contraseña con sesión (C-03) todavía no cierra las demás sesiones] → El mecanismo de D7 queda listo para usarlo en otro change.

## Migration Plan

1. `V5__recuperacion_password.sql` se aplica con el migrator. Es aditiva.
2. Variables nuevas y opcionales: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` y `MAIL_FROM`. Sin `SMTP_HOST`, el email queda deshabilitado.
3. **Rollback**: revertir la imagen. La tabla puede quedar; cualquier corrección va en una migración nueva.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI) — 2026-11-26. Las pantallas "olvidé mi contraseña" y "restablecer" son de C-08, con su propio mockup.
