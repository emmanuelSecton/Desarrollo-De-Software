# Integración: WhatsApp Business Platform (Cloud API)

> Verificar precios, límites y políticas vigentes en la documentación oficial de Meta antes de implementar: cambian con frecuencia. Lo que sigue es el diseño, no una cotización.

## Propósito

Enviar recordatorios de turno con botones de confirmación, recibir las respuestas del paciente y enviar confirmaciones de reserva, cancelación y reprogramación.

## Requisitos de alta (por consultorio; los hace **cada odontólogo**, DD-12)

1. Cuenta de Meta Business, con verificación del negocio recomendada para subir los límites de envío.
2. Una WhatsApp Business Account (WABA) y un número de teléfono dedicado. El número no puede estar usándose en la app de WhatsApp común al mismo tiempo.
3. App de Meta con el producto WhatsApp y un token de sistema permanente.
4. Webhook configurado hacia `https://<instancia>/api/v1/webhooks/whatsapp`, suscripto a `messages`.
5. Plantillas aprobadas (ver abajo).

El odontólogo hace este proceso siguiendo una **guía paso a paso** del producto (PA-04, DD-12). Luego carga en el panel (`/configuracion/whatsapp`) el `phone_number_id`, el token de sistema y el App Secret. El sistema:
- los guarda **cifrados en la base** (AES-GCM con `CRYPTO_KEY`), no en variables de entorno;
- valida las credenciales con una llamada de prueba a la Graph API;
- muestra el estado de aprobación de cada plantilla.

**Demo**: se usa el número de prueba gratuito de la Cloud API, que solo envía a destinatarios registrados, más plantillas de prueba.

## Plantillas (categoría "utilidad")

| Nombre | Uso | Variables | Botones |
|---|---|---|---|
| `recordatorio_turno` | Recordatorio a 48 h | nombre, fecha, hora, consultorio | Respuesta rápida: "Confirmo" / "Cancelar" |
| `confirmacion_reserva` | Después de una reserva online o interna | nombre, fecha, hora, enlace | URL: "Ver mi turno" |
| `turno_cancelado` | Cancelación por el consultorio | nombre, fecha, hora, teléfono | — |
| `turno_reprogramado` | Reprogramación | nombre, fecha nueva, hora nueva, enlace | URL |
| `oferta_hueco` | Hueco liberado ofrecido a pacientes en lista de espera (DD-10) | nombre, prestación, fecha, hora, minutos para responder, consultorio | Respuesta rápida: "Lo tomo" |

**Sobre `oferta_hueco`**: se redacta como respuesta a la solicitud del paciente ("Te anotaste en la lista de espera…") para que Meta la apruebe como **utilidad**. Si la clasifica como marketing, el costo por mensaje sube (R-06). Hay que someterla a aprobación al principio del proyecto (C-15). La respuesta al ganador ("¡Listo! Tu turno quedó confirmado…") y a los que llegaron tarde ("Ese turno ya fue tomado, seguís en la lista") son mensajes de servicio dentro de la ventana de 24 h que abre su propia respuesta.

El payload de cada botón de respuesta rápida lleva un identificador opaco del turno (por ejemplo `CONF:<token corto>`) para resolverlo sin depender del texto.

## Ventana de servicio de 24 h

- Fuera de la ventana solo se pueden enviar **plantillas**.
- Cuando el paciente responde, se abre una ventana de 24 h en la que se le puede contestar con mensajes libres. El sistema la usa para el acuse ("¡Gracias! Tu turno quedó confirmado").

## Webhooks

- Verificación inicial: `GET` con `hub.mode`, `hub.verify_token` y `hub.challenge`.
- Eventos: `POST` con `statuses` (sent, delivered, read, failed) y `messages` (respuestas del paciente).
- Seguridad: validar `X-Hub-Signature-256` (HMAC-SHA256 con el App Secret) antes de procesar.
- Idempotencia: Meta reintenta; se deduplica por `message.id` o `status.id`.
- Respuesta rápida: devolver HTTP 200 enseguida y procesar en segundo plano.

## Costos

- Meta cobra por mensaje de plantilla entregado, según la categoría (utilidad, marketing, autenticación) y el país del destinatario. Las respuestas dentro de la ventana de servicio no tienen cargo.
- Cada consultorio paga los mensajes de su WABA, salvo que el modelo de negocio los incluya (PA-07).
- Estimación a validar: un recordatorio por turno más una confirmación, ≈ 2 plantillas de utilidad por turno, más hasta 5 plantillas `oferta_hueco` por cada hueco ofrecido.

## Errores y degradación

| Situación | Manejo |
|---|---|
| Número no registrado en WhatsApp | `failed` → respaldo por email (RN-NO-03) |
| Plantilla pausada o rechazada | Alerta al odontólogo; respaldo por email |
| Límite de envío alcanzado | Reintento con backoff; la notificación queda `PENDIENTE` |
| Token vencido o inválido | Alerta crítica en logs y en el panel; respaldo por email |
| El paciente responde con texto libre | Se guarda y se muestra en la bandeja; no cambia el estado del turno |

## Consentimiento

El paciente acepta recibir WhatsApp al reservar o en el alta presencial (`acepta_whatsapp`, RN-DP-01). Puede revocarlo respondiendo "BAJA"; el sistema actualiza la marca y deja de enviarle mensajes por ese canal.
