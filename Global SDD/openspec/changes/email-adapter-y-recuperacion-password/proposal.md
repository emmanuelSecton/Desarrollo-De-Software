# Proposal — email-adapter-y-recuperacion-password (C-04)

> Governance: `CHANGES.md` lo marca **ALTO**, pero toca autenticación, que la tabla de governance trata como **CRÍTICO**. Se aplica el criterio más estricto: solo planificación hasta que el usuario lo apruebe.

## Why

Hoy un usuario interno que olvida su contraseña no tiene forma de recuperarla sin que alguien toque la base (US-003). Además, el sistema todavía no puede enviar emails, y varios changes posteriores los necesitan: confirmación de reserva (C-20), respaldo de recordatorios cuando falla WhatsApp (RN-NO-03) y la pantalla "olvidé mi contraseña" (C-08). C-04 resuelve las dos cosas: un envío de email desacoplado del proveedor (PA-10 sigue abierta) y la recuperación de contraseña por enlace con vencimiento.

## What Changes

- **Envío de email** en el módulo `notificaciones`: un puerto `MensajeriaPort` con un adaptador SMTP (cualquier proveedor transaccional ofrece SMTP), plantillas de texto y HTML simples, y hasta 3 intentos con espera creciente. Variables `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `MAIL_FROM`. Sin `SMTP_HOST`, el envío queda deshabilitado y solo se registra en el log que no se envió, sin destinatario ni contenido.
- **Tabla `token_recuperacion`** (migración `V5`): guarda solo el hash SHA-256 del token, vence a la hora y sirve una sola vez.
- **`POST /api/v1/auth/password/recover`**: recibe un email y responde siempre `202` con el mismo cuerpo, exista o no la cuenta (sin enumeración). Si la cuenta existe y está activa, invalida los tokens anteriores, crea uno nuevo y manda el enlace por email.
- **`POST /api/v1/auth/password/reset`**: recibe el token y la contraseña nueva; aplica la política de C-03, limpia el cambio obligatorio y el bloqueo por intentos, marca el token como usado y **cierra todas las sesiones abiertas** del usuario.
- El email se envía **después del commit y en segundo plano**, para que la respuesta tarde lo mismo exista o no la cuenta.
- Auditoría del pedido (`SOLICITAR_RECUPERACION`, nueva acción del catálogo) y del restablecimiento (`MODIFICAR_ENTIDAD` con referencia `recuperacion-password`), sin email ni token.
- El límite de peticiones por IP de C-03 se extiende a `recover` y `reset`.
- En desarrollo, `docker-compose.override.yml` suma Mailpit (SMTP de prueba con bandeja web en `127.0.0.1:8025`).

Fuera de alcance: pantallas "olvidé mi contraseña" y "restablecer" (C-08, con mockup), envío por WhatsApp (C-15), cola persistente de reintentos de notificaciones (C-14), elección del proveedor de email (PA-10).

## Capabilities

### New Capabilities
- `notificaciones`: envío de mensajes salientes al personal y a los pacientes. En este change cubre el email por SMTP: configuración, reintentos, modo deshabilitado y protección de datos en los logs.

### Modified Capabilities
- `identidad`: se agrega la recuperación de contraseña por email y se amplía el requisito "Límite de peticiones por IP" para incluir `recover` y `reset`.

## Impact

- **Backend**: módulo `notificaciones` (puerto y adaptador SMTP) y módulo `identidad` (servicio de recuperación, endpoints, cierre de sesiones). `identidad` pasa a depender de `notificaciones.application`. `auditoria` suma la acción `SOLICITAR_RECUPERACION`, sin migración.
- **Base**: migración `V5__recuperacion_password.sql`; el rol runtime recibe `SELECT, INSERT, UPDATE` sobre `token_recuperacion`.
- **Dependencias**: `spring-boot-starter-mail`; en tests, `greenmail-junit5` (servidor SMTP en memoria).
- **Configuración**: variables `SMTP_*` y `MAIL_FROM` en compose y README (y a agregar en `.env.example`).
- **Frontend**: ninguno; C-08 consume los endpoints.
- **Desbloquea**: junto con C-05, C-06 y C-07, el C-08. También deja listo el email para C-14 y C-20.
