# Tasks: email-adapter-y-recuperacion-password (C-04)

Convenciones de apply:
- **Governance CRÍTICO** (auth): no se empieza ninguna tarea sin la aprobación explícita del usuario.
- **TDD estricto**: RED → GREEN → TRIANGULAR (caso feliz + borde) → REFACTOR, con los tests en verde tras cada paso.
- **Base real** con Testcontainers; **SMTP real** con GreenMail; datos ficticios `*@consultorio.test`.

## 1. Safety net y dependencias

- [x] 1.1 Ejecutar `./gradlew build` sobre `main` antes de tocar nada y registrar "N/N tests verdes"; si algo falla, frenar y reportar. Verificación: baseline anotado en el resumen de apply.
- [x] 1.2 Agregar `spring-boot-starter-mail` y, en tests, `com.icegreen:greenmail-junit5` con versión fija. Verificación: `./gradlew dependencies` los resuelve y el build sigue verde.

## 2. Envío de email (`notificaciones`)

- [x] 2.1 Crear `MensajeriaPort`, `MensajeEmail` (sin datos en `toString()`) y `EmailSmtpAdapter` con la configuración `spring.mail.*` desde `SMTP_*` y `MAIL_FROM`. Verificación: `EmailSmtpAdapterTest` con GreenMail recibe un mensaje desde `MAIL_FROM` con asunto, texto y HTML.
- [x] 2.2 Implementar los 3 intentos con espera (D2) y los logs sin datos personales. Verificación: `EmailSmtpAdapterTest` cubre un fallo seguido de éxito (sin log de error) y 3 fallos (un solo `ERROR`, sin destinatario ni enlace en ningún log).
- [x] 2.3 Implementar `EmailDeshabilitadoAdapter` para `SMTP_HOST` vacío. Verificación: `EnvioDeshabilitadoTest` arranca sin `SMTP_HOST`, envía sin fallar y el log advierte sin datos del mensaje.

## 3. Migración `token_recuperacion`

- [x] 3.1 Crear `V5__recuperacion_password.sql` (D3) y actualizar `MigracionBaselineTest` a `"5:recuperacion password"`. Verificación: `EsquemaTokenRecuperacionTest` comprueba columnas, unicidad del hash, `CHECK` de vencimiento, clave foránea, y que el rol runtime lee, inserta y actualiza pero no borra.

## 4. Servicio de recuperación (`identidad`)

- [x] 4.1 Agregar `SOLICITAR_RECUPERACION` a `AccionAuditoria` e implementar el token (generación, SHA-256) y el repositorio JDBC. Verificación: `TokenDeRecuperacionTest` (unitario) genera tokens de 43 caracteres distintos y un hash de 64 hexadecimales; `RepositorioTokensTest` guarda, busca por hash e invalida los anteriores.
- [x] 4.2 Implementar `ServicioDeRecuperacion.solicitar` (D4) y el envío después del commit en segundo plano (D5). Verificación: `SolicitudDeRecuperacionTest` con base real y el doble de `MensajeriaPort` cubre que una cuenta activa recibe un email con enlace a `APP_BASE_URL/restablecer?token=…`, cuyo hash coincide con el guardado; que un email inexistente o inactivo no envía nada; que un pedido repetido invalida el token anterior; y que el evento `SOLICITAR_RECUPERACION` no contiene email ni token.
- [x] 4.3 Implementar `ServicioDeRecuperacion.restablecer` (D6). Verificación: `RestablecimientoTest` con `RelojDePrueba` cubre el éxito (nuevo hash cost 12, sin cambio obligatorio, desbloqueado, token usado y evento auditado), el token vencido a la hora, el token reutilizado y el inexistente con el mismo mensaje, y la contraseña inválida que deja el token vigente.

## 5. Endpoints, sesiones y límites

- [x] 5.1 Agregar `POST /auth/password/recover` y `POST /auth/password/reset` a `AuthController` y a `RutasDeSeguridad` (D8). Verificación: `RecuperacionPasswordControllerTest` muestra `202` con cuerpo idéntico para un email existente, uno inexistente y uno inactivo; `reset` `204` y luego login con la nueva; `reset` con token malo `400`; ambos `403` sin CSRF.
- [x] 5.2 Implementar el cierre de sesiones (D7). Verificación: `CierreDeSesionesTest` (servidor HTTP real) inicia sesión dos veces con el mismo usuario, restablece la contraseña y comprueba que las dos cookies anteriores responden `401` en `/auth/me`; un login anterior de otro usuario sigue activo.
- [x] 5.3 Extender el límite por IP a `recover` y `reset`. Verificación: `LimiteDePeticionesTest` suma un caso donde el `recover` excedido responde `429` y no envía email.

## 6. Configuración y verificación integral

- [x] 6.1 Documentar `SMTP_*` y `MAIL_FROM` en `deploy/docker-compose.yml` y el README, y sumar Mailpit a `docker-compose.override.yml` (solo `127.0.0.1`). Verificación: `ConfiguracionFlywayComposeTest` sigue verde y el README nombra las variables.
- [x] 6.2 Ejecutar `./gradlew build` completo: todos los tests en verde, JaCoCo ≥ 80 % de líneas y ArchUnit sin violaciones. Verificación: totales en el resumen de apply.
- [x] 6.3 Levantar el stack con `docker compose` y Mailpit y recorrer: `recover` de una cuenta existente e inexistente (mismo `202`), email visible en Mailpit, `reset` con el enlace, login con la nueva y reuso del token `400`. Verificación: secuencia y códigos anotados en el resumen de apply.

## Workflow follow-up

- Agregar `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` y `MAIL_FROM` a `.env.example` (el agente no tiene permiso sobre ese archivo).
- Revisión humana del PR (CRÍTICO), merge, sincronizar `openspec/specs/notificaciones` e `identidad`, archivar y marcar C-04 en `CHANGES.md`.
