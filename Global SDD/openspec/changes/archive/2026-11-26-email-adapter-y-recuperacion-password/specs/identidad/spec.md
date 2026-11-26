# Spec Delta — identidad

## ADDED Requirements

### Requirement: Pedido de recuperación sin enumeración de cuentas
`POST /api/v1/auth/password/recover` SHALL aceptar un email y responder siempre `202` con el mismo cuerpo, exista o no la cuenta y esté o no activa. Solo si la cuenta existe y está activa MUST enviarse un email con un enlace de recuperación. El tiempo de respuesta MUST NOT depender de que la cuenta exista.

#### Scenario: Email de una cuenta activa
- **WHEN** se pide recuperar la contraseña de una cuenta activa
- **THEN** la respuesta es `202` y se envía un email con un enlace a `APP_BASE_URL/restablecer?token=…`

#### Scenario: Email inexistente o cuenta inactiva
- **WHEN** se pide recuperar la contraseña de un email que no existe o de una cuenta inactiva
- **THEN** la respuesta es `202` con el mismo cuerpo y no se envía ningún email

### Requirement: Token de recuperación de un solo uso
El token SHALL tener al menos 256 bits aleatorios, guardarse solo como hash, vencer a la hora y servir una sola vez. Un pedido nuevo MUST invalidar los tokens anteriores sin usar del mismo usuario.

#### Scenario: Token guardado como hash
- **WHEN** se genera un token
- **THEN** la base guarda su hash y nunca el token en claro

#### Scenario: Pedido repetido
- **GIVEN** un usuario que pidió recuperar la contraseña
- **WHEN** vuelve a pedirlo
- **THEN** el enlace del primer email deja de servir y solo sirve el del segundo

### Requirement: Restablecimiento de contraseña con token
`POST /api/v1/auth/password/reset` SHALL recibir el token y la contraseña nueva, validarla con la política de contraseñas y responder `204`. Al restablecerla MUST limpiar el cambio obligatorio y el bloqueo por intentos, marcar el token como usado y cerrar todas las sesiones abiertas del usuario. Un token inválido, vencido o ya usado MUST responder `400` con un mismo mensaje genérico.

#### Scenario: Restablecimiento correcto
- **GIVEN** un token válido de un usuario bloqueado por intentos y con sesiones abiertas
- **WHEN** se envía el token con una contraseña nueva válida
- **THEN** la respuesta es `204`, el usuario puede iniciar sesión con la nueva, ya no está bloqueado y sus sesiones anteriores responden `401`

#### Scenario: Token vencido
- **GIVEN** un token generado hace más de una hora
- **WHEN** se lo usa
- **THEN** la respuesta es `400` y la contraseña no cambia

#### Scenario: Token reutilizado
- **GIVEN** un token ya usado
- **WHEN** se lo usa otra vez
- **THEN** la respuesta es `400` con el mismo mensaje que un token inexistente

#### Scenario: Contraseña nueva inválida
- **WHEN** se envía un token válido con una contraseña de menos de 10 caracteres
- **THEN** la respuesta es `400` con el error de la contraseña y el token sigue sirviendo

### Requirement: Auditoría de la recuperación
Cada pedido de recuperación de una cuenta existente SHALL registrar `SOLICITAR_RECUPERACION`, y cada restablecimiento `MODIFICAR_ENTIDAD` con referencia `recuperacion-password`, ambos con el id del usuario. Ningún evento MUST incluir el email ni el token.

#### Scenario: Recuperación completa auditada
- **WHEN** un usuario pide recuperar la contraseña y la restablece
- **THEN** existen los dos eventos con su id, sin email ni token en ninguna columna

## MODIFIED Requirements

### Requirement: Límite de peticiones por IP
El sistema SHALL limitar por dirección IP las peticiones a `POST /api/v1/auth/login`, `POST /api/v1/auth/password/recover`, `POST /api/v1/auth/password/reset` y `/api/v1/public/**`. Al superar el límite MUST responder `429` en formato RFC 7807 con cabecera `Retry-After`. Los límites SHALL ser configurables por variables de entorno.

#### Scenario: Exceso de intentos de login desde una IP
- **GIVEN** una IP que agotó su cupo de login
- **WHEN** envía otro `POST /api/v1/auth/login`
- **THEN** la respuesta es `429` con `Retry-After` y no se evalúan las credenciales

#### Scenario: IPs independientes
- **GIVEN** una IP que agotó su cupo
- **WHEN** otra IP intenta un login
- **THEN** su petición se procesa normalmente

#### Scenario: Exceso de pedidos de recuperación
- **GIVEN** una IP que agotó su cupo
- **WHEN** envía otro `POST /api/v1/auth/password/recover`
- **THEN** la respuesta es `429` y no se envía ningún email

### Requirement: Rutas autenticadas por defecto
Toda ruta bajo `/api/v1` SHALL exigir sesión salvo las públicas: `/api/v1/auth/login`, `/api/v1/auth/csrf`, `/api/v1/auth/password/recover`, `/api/v1/auth/password/reset`, `/api/v1/public/**` y `/api/v1/webhooks/**`. Sin sesión, el sistema MUST responder `401` en formato RFC 7807. `/actuator/health` y la SPA siguen siendo públicos.

#### Scenario: Ruta protegida sin sesión
- **WHEN** se pide una ruta de negocio bajo `/api/v1` sin sesión
- **THEN** la respuesta es `401` con `Content-Type: application/problem+json`

#### Scenario: Recursos públicos
- **WHEN** se pide `/actuator/health`, `/` o un archivo estático de la SPA sin sesión
- **THEN** la respuesta no es `401`
