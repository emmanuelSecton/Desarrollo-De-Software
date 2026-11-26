# identidad Specification

## Purpose

Define cómo los usuarios internos del consultorio (odontólogo y secretarias) se identifican, mantienen su sesión y quedan autorizados por rol, con las protecciones exigidas por RN-SEG-01 a RN-SEG-04 y la Ley 25.326.

## Requirements

### Requirement: Usuarios internos con un único odontólogo activo
El sistema SHALL guardar usuarios internos con email único sin distinguir mayúsculas, rol `ODONTOLOGO` o `SECRETARIA`, estado activo y último login. La base MUST impedir que haya más de un `ODONTOLOGO` activo. Los usuarios no se borran: la baja es lógica.

#### Scenario: Email duplicado con otra capitalización
- **GIVEN** existe el usuario `dra@consultorio.test`
- **WHEN** se intenta crear otro usuario con `DRA@Consultorio.test`
- **THEN** la base rechaza la inserción

#### Scenario: Segundo odontólogo activo
- **GIVEN** existe un `ODONTOLOGO` activo
- **WHEN** se intenta insertar o activar otro `ODONTOLOGO`
- **THEN** la base rechaza la operación

#### Scenario: El rol runtime no puede borrar usuarios
- **WHEN** la aplicación, con el rol runtime de la base, ejecuta un `DELETE` sobre `usuario`
- **THEN** la base lo rechaza por falta de permisos

### Requirement: Alta del odontólogo inicial
Al arrancar una instancia sin ningún `ODONTOLOGO`, el sistema SHALL crear uno con el email de `ADMIN_INITIAL_EMAIL`, la contraseña temporal de `ADMIN_INITIAL_PASSWORD` y el cambio de contraseña marcado como obligatorio. Si ya existe un `ODONTOLOGO`, el sistema MUST ignorar ambas variables. Ninguna de las dos MUST aparecer en logs.

#### Scenario: Primer arranque
- **GIVEN** una base sin usuarios y ambas variables definidas
- **WHEN** la aplicación arranca
- **THEN** existe un `ODONTOLOGO` activo con ese email, que debe cambiar la contraseña en su primer login

#### Scenario: Arranques posteriores
- **GIVEN** ya existe un `ODONTOLOGO`
- **WHEN** la aplicación arranca con otras credenciales iniciales
- **THEN** no se crea ni modifica ningún usuario

#### Scenario: Instancia vacía sin credenciales iniciales
- **GIVEN** una base sin usuarios y `ADMIN_INITIAL_EMAIL` o `ADMIN_INITIAL_PASSWORD` vacía
- **WHEN** la aplicación arranca
- **THEN** el arranque falla con un mensaje que nombra la variable faltante, sin mostrar valores

### Requirement: Inicio de sesión con email y contraseña
`POST /api/v1/auth/login` SHALL aceptar email y contraseña y, si son válidos para un usuario activo y no bloqueado, abrir una sesión de servidor y responder `200` con id, nombre, email, rol y si debe cambiar la contraseña. El identificador de sesión MUST cambiar al iniciar sesión. La contraseña MUST guardarse solo como hash BCrypt con cost 12 o mayor.

#### Scenario: Login correcto
- **WHEN** un usuario activo envía credenciales válidas con un token CSRF válido
- **THEN** la respuesta es `200` con sus datos y rol, se emite una cookie de sesión nueva y se actualiza su último login

#### Scenario: Fijación de sesión
- **GIVEN** el cliente ya tenía una cookie de sesión anónima
- **WHEN** inicia sesión correctamente
- **THEN** la cookie de sesión anterior deja de ser válida

#### Scenario: Hash almacenado
- **WHEN** se guarda una contraseña
- **THEN** la columna contiene un hash BCrypt con cost ≥ 12 y nunca la contraseña en claro

### Requirement: Error de login sin enumeración de cuentas
Ante credenciales inválidas, email inexistente, usuario inactivo o cuenta bloqueada, el sistema SHALL responder `401` en formato RFC 7807 con el mismo `type`, `title` y `detail`, sin indicar cuál fue la causa.

#### Scenario: Email inexistente y contraseña incorrecta responden igual
- **WHEN** se intenta un login con un email inexistente y otro con un email válido y contraseña incorrecta
- **THEN** ambas respuestas son `401` con cuerpos idénticos

#### Scenario: Usuario inactivo
- **GIVEN** un usuario con `activo = false`
- **WHEN** envía su contraseña correcta
- **THEN** la respuesta es `401` idéntica a la de credenciales inválidas

### Requirement: Bloqueo por intentos fallidos
Tras 5 intentos fallidos sobre una misma cuenta dentro de 15 minutos, el sistema SHALL bloquearla durante 15 minutos (RN-SEG-03). Durante el bloqueo, incluso la contraseña correcta MUST ser rechazada. Un login exitoso MUST reiniciar el contador.

#### Scenario: Quinto intento fallido
- **GIVEN** una cuenta con 4 intentos fallidos en los últimos 15 minutos
- **WHEN** falla un quinto intento
- **THEN** la cuenta queda bloqueada 15 minutos

#### Scenario: Contraseña correcta durante el bloqueo
- **GIVEN** una cuenta bloqueada hace 10 minutos
- **WHEN** envía la contraseña correcta
- **THEN** la respuesta es `401` y no se abre sesión

#### Scenario: Fin del bloqueo
- **GIVEN** una cuenta bloqueada hace 15 minutos o más
- **WHEN** envía la contraseña correcta
- **THEN** inicia sesión y el contador vuelve a cero

#### Scenario: Fallos fuera de la ventana
- **GIVEN** 4 intentos fallidos con más de 15 minutos de antigüedad
- **WHEN** falla un nuevo intento
- **THEN** la cuenta no se bloquea

### Requirement: Cookie de sesión y expiración por inactividad
La cookie de sesión SHALL ser `HttpOnly`, `Secure` y `SameSite=Strict`. La sesión MUST expirar tras 30 minutos sin peticiones (RN-SEG-04); una petición posterior responde `401`.

#### Scenario: Atributos de la cookie
- **WHEN** un login es exitoso
- **THEN** la cookie de sesión tiene `HttpOnly`, `Secure` y `SameSite=Strict`

#### Scenario: Sesión inactiva
- **GIVEN** una sesión sin peticiones durante más de 30 minutos
- **WHEN** el cliente pide `GET /api/v1/auth/me`
- **THEN** la respuesta es `401`

### Requirement: Cierre de sesión y usuario actual
`POST /api/v1/auth/logout` SHALL invalidar la sesión y responder `204`. `GET /api/v1/auth/me` SHALL devolver los datos del usuario de la sesión, o `401` sin sesión.

#### Scenario: Logout
- **WHEN** un usuario con sesión hace logout
- **THEN** la respuesta es `204` y una petición posterior con la misma cookie responde `401`

#### Scenario: Usuario actual
- **WHEN** un usuario con sesión pide `GET /api/v1/auth/me`
- **THEN** recibe su id, nombre, email, rol y la marca de cambio de contraseña pendiente

### Requirement: Protección CSRF para la SPA
Toda petición que modifica estado (`POST`, `PUT`, `PATCH`, `DELETE`) bajo `/api/v1` SHALL exigir un token CSRF válido en la cabecera `X-XSRF-TOKEN`, incluido el login. `GET /api/v1/auth/csrf` SHALL entregar el token también en una cookie legible por la SPA. Los webhooks firmados MUST quedar exentos.

#### Scenario: POST sin token CSRF
- **WHEN** se envía `POST /api/v1/auth/login` sin cabecera `X-XSRF-TOKEN`
- **THEN** la respuesta es `403` en formato RFC 7807

#### Scenario: Obtención del token
- **WHEN** la SPA pide `GET /api/v1/auth/csrf`
- **THEN** recibe el token y una cookie `XSRF-TOKEN` que puede reenviar como cabecera

### Requirement: Cambio de contraseña obligatorio
Mientras un usuario tenga el cambio de contraseña pendiente, el sistema SHALL responder `403` a toda ruta autenticada salvo `GET /auth/me`, `POST /auth/logout` y `POST /auth/password`. El cambio MUST exigir la contraseña actual y una nueva distinta, de 10 a 72 caracteres.

#### Scenario: Ruta bloqueada hasta cambiar la contraseña
- **GIVEN** un usuario con cambio de contraseña pendiente y sesión activa
- **WHEN** pide cualquier otra ruta autenticada
- **THEN** la respuesta es `403` con un `type` que indica que debe cambiar la contraseña

#### Scenario: Cambio exitoso
- **WHEN** envía la contraseña actual correcta y una nueva válida
- **THEN** la respuesta es `204`, la marca se limpia y puede usar el resto de la API

#### Scenario: Contraseña actual incorrecta o nueva inválida
- **WHEN** la contraseña actual no coincide, o la nueva es igual a la actual o mide menos de 10 caracteres
- **THEN** la respuesta es `400` en formato RFC 7807 y la contraseña no cambia

### Requirement: Rutas autenticadas por defecto
Toda ruta bajo `/api/v1` SHALL exigir sesión salvo las públicas: `/api/v1/auth/login`, `/api/v1/auth/csrf`, `/api/v1/auth/password/recover`, `/api/v1/public/**` y `/api/v1/webhooks/**`. Sin sesión, el sistema MUST responder `401` en formato RFC 7807. `/actuator/health` y la SPA siguen siendo públicos.

#### Scenario: Ruta protegida sin sesión
- **WHEN** se pide una ruta de negocio bajo `/api/v1` sin sesión
- **THEN** la respuesta es `401` con `Content-Type: application/problem+json`

#### Scenario: Recursos públicos
- **WHEN** se pide `/actuator/health`, `/` o un archivo estático de la SPA sin sesión
- **THEN** la respuesta no es `401`

### Requirement: Autorización por rol
El sistema SHALL autorizar las operaciones según la matriz RBAC de `03_actores_y_roles.md`. Una operación restringida a `ODONTOLOGO`, como las de historia clínica, MUST responder `403` en formato RFC 7807 cuando la pide una `SECRETARIA` (RN-SEG-02).

#### Scenario: Secretaria en una ruta de historia clínica
- **GIVEN** una secretaria con sesión
- **WHEN** invoca una operación restringida a `ODONTOLOGO`
- **THEN** la respuesta es `403` y la operación no se ejecuta

#### Scenario: Odontólogo en la misma ruta
- **WHEN** el odontólogo invoca esa operación
- **THEN** la operación se ejecuta

### Requirement: Límite de peticiones por IP
El sistema SHALL limitar por dirección IP las peticiones a `POST /api/v1/auth/login` y a `/api/v1/public/**`. Al superar el límite MUST responder `429` en formato RFC 7807 con cabecera `Retry-After`. Los límites SHALL ser configurables por variables de entorno.

#### Scenario: Exceso de intentos de login desde una IP
- **GIVEN** una IP que agotó su cupo de login
- **WHEN** envía otro `POST /api/v1/auth/login`
- **THEN** la respuesta es `429` con `Retry-After` y no se evalúan las credenciales

#### Scenario: IPs independientes
- **GIVEN** una IP que agotó su cupo
- **WHEN** otra IP intenta un login
- **THEN** su petición se procesa normalmente

### Requirement: Auditoría de inicios de sesión
Cada login exitoso SHALL registrar un evento `LOGIN` con el usuario como actor, y cada login rechazado un evento `LOGIN_FALLIDO` con un código de motivo. Ninguno MUST incluir el email ni la contraseña. El evento de un login fallido MUST persistir aunque el login no tenga éxito.

#### Scenario: Login exitoso auditado
- **WHEN** un usuario inicia sesión
- **THEN** existe un evento `LOGIN` con actor `USUARIO`, su id y la IP de la petición

#### Scenario: Login fallido auditado sin datos personales
- **WHEN** falla un login con email inexistente o contraseña incorrecta
- **THEN** existe un evento `LOGIN_FALLIDO` con actor `SISTEMA` y un `motivo_codigo`, sin el email ni la contraseña en ninguna columna

### Requirement: Cabeceras de seguridad HTTP
Toda respuesta SHALL incluir `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `X-Frame-Options: DENY` y, sobre HTTPS, `Strict-Transport-Security`.

#### Scenario: Cabeceras presentes
- **WHEN** se pide cualquier ruta
- **THEN** la respuesta incluye las cabeceras de seguridad indicadas
