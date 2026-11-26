# Spec Delta — notificaciones

## Purpose

Define cómo el sistema envía mensajes salientes, empezando por el email: independiente del proveedor, con reintentos acotados, sin datos personales en los logs y sin romper la aplicación cuando el envío no está configurado.

## ADDED Requirements

### Requirement: Envío de email independiente del proveedor
El sistema SHALL enviar emails por SMTP con la configuración de `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` y el remitente `MAIL_FROM`. Cada email MUST llevar una versión en texto plano y otra en HTML con el mismo contenido. Cambiar de proveedor MUST requerir solo cambiar esas variables.

#### Scenario: Email enviado
- **GIVEN** un servidor SMTP configurado
- **WHEN** el sistema envía un email a `usuario@consultorio.test`
- **THEN** el servidor recibe un mensaje desde `MAIL_FROM` con asunto, cuerpo de texto y cuerpo HTML

### Requirement: Reintentos acotados
Si el servidor SMTP falla, el sistema SHALL reintentar el envío hasta 3 intentos en total, con espera creciente entre intentos. Si los 3 fallan, MUST registrar el fallo en el log y no relanzar el error a quien pidió el envío.

#### Scenario: Falla transitoria
- **GIVEN** un servidor SMTP que falla en el primer intento y responde en el segundo
- **WHEN** el sistema envía un email
- **THEN** el email llega y no se registra ningún fallo

#### Scenario: Servidor caído
- **GIVEN** un servidor SMTP que no responde
- **WHEN** el sistema envía un email
- **THEN** se hacen 3 intentos y se registra un único error en el log

### Requirement: Envío deshabilitado sin configuración
Si `SMTP_HOST` está vacío, la aplicación SHALL arrancar igual y cada envío MUST registrar en el log una advertencia de que el email no se envió, sin destinatario, asunto ni contenido.

#### Scenario: Instancia sin SMTP
- **GIVEN** una instancia sin `SMTP_HOST`
- **WHEN** el sistema intenta enviar un email
- **THEN** no falla y el log dice que el envío está deshabilitado, sin datos del mensaje

### Requirement: Logs sin datos personales ni contenido
Ningún log del envío de email SHALL incluir la dirección del destinatario, el asunto, el cuerpo ni enlaces (Ley 25.326). Los logs MUST limitarse al tipo de mensaje, el número de intento y el tipo de error.

#### Scenario: Error de envío
- **WHEN** falla el envío de un email de recuperación
- **THEN** el log incluye el tipo de mensaje y el error, pero no el email ni el enlace
