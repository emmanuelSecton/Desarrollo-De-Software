# Design — identidad-auth-y-rbac (C-03)

## Context

- C-01 dejó el monolito modular con el módulo `identidad` vacío (solo `package-info.java` por capa), ArchUnit verificando capas y límites entre módulos, errores RFC 7807 desde `ManejadorGlobalDeErrores`, un `Clock` UTC inyectable y la SPA servida desde el mismo origen. Spring Security **no** está en el build.
- C-02 dejó `AuditoriaService` y `EventoAuditoria` como API pública del módulo `auditoria`, con `LOGIN` y `LOGIN_FALLIDO` en el catálogo. El evento se escribe en `BEFORE_COMMIT` de la misma transacción: **si la transacción se revierte, el evento se pierde**. El `detalle` solo admite claves de una whitelist (`usuario_id`, `motivo_codigo`, `referencia`, …) y rechaza `email` y `password`.
- La base tiene un rol migrator (Flyway) y un rol runtime (`${runtime_role}`); en compose la app corre con el runtime y Flyway corre en el servicio `migrate`.
- `citext` y `pgcrypto` ya están habilitadas en `V1__baseline.sql`.
- Una sola instancia de la aplicación por consultorio (DD-01): no hay balanceo entre réplicas.

## Goals / Non-Goals

**Goals:**
- Que todo `/api/v1/**` quede cerrado por defecto y que abrir una ruta pública sea una decisión explícita.
- Que los módulos futuros declaren permisos con una anotación por método, sin depender de clases internas de `identidad`.
- Que bloqueo, expiración y forzado de cambio de contraseña sean testeables con el reloj inyectado, sin esperas reales.

**Non-Goals:**
- Sesiones compartidas entre réplicas ni persistencia de sesiones ante reinicios.
- Confiar en `X-Forwarded-For` (C-35 define el proxy; hasta entonces la IP es la del socket, igual que en C-02).
- Política de complejidad de contraseñas más allá del largo.

## Decisions

### D1 — Tabla `usuario` con `CHECK` en vez de `ENUM` de PostgreSQL
`V3__identidad.sql` crea:

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `UUID PK DEFAULT gen_random_uuid()` | |
| `email` | `CITEXT NOT NULL UNIQUE` | |
| `password_hash` | `VARCHAR(100) NOT NULL` | BCrypt (`$2a$12$…`, 60 caracteres) |
| `nombre` | `VARCHAR(120) NOT NULL` | |
| `rol` | `VARCHAR(20) NOT NULL CHECK (rol IN ('ODONTOLOGO','SECRETARIA'))` | |
| `activo` | `BOOLEAN NOT NULL DEFAULT TRUE` | |
| `debe_cambiar_password` | `BOOLEAN NOT NULL DEFAULT TRUE` | |
| `intentos_fallidos` | `INT NOT NULL DEFAULT 0` | |
| `primer_fallo_at` | `TIMESTAMPTZ` | inicio de la ventana de 15 min |
| `bloqueado_hasta` | `TIMESTAMPTZ` | |
| `ultimo_login_at` | `TIMESTAMPTZ` | |
| `creado_at` | `TIMESTAMPTZ NOT NULL` | viene del `Clock`, sin `DEFAULT now()` |

- Unicidad del odontólogo activo: índice único parcial `CREATE UNIQUE INDEX uq_usuario_odontologo_activo ON usuario ((rol)) WHERE rol = 'ODONTOLOGO' AND activo`. Garantiza **como máximo uno**; que haya **al menos uno** lo garantizan el alta inicial (D2) y la regla de C-05 que impide dar de baja al odontólogo.
- Grants: `SELECT, INSERT, UPDATE ON usuario TO ${runtime_role}`; sin `DELETE` (baja lógica).
- *Alternativa descartada*: `CREATE TYPE rol_usuario AS ENUM`. Agregar un valor requiere `ALTER TYPE` y no se puede quitar; C-02 ya eligió `CHECK`/texto por la misma razón.

### D2 — Alta inicial con `ADMIN_INITIAL_EMAIL` + `ADMIN_INITIAL_PASSWORD`
Un `ApplicationRunner` de `identidad/infrastructure` corre al arrancar: si no existe ningún `ODONTOLOGO`, exige ambas variables, inserta el usuario con `debe_cambiar_password = true` (`INSERT … ON CONFLICT DO NOTHING`) y no loguea ningún valor. Si ya existe, no lee las variables.
- *Alternativa descartada 1*: generar una contraseña aleatoria y mostrarla en el log de arranque. Deja un secreto en los logs del hosting, contra la regla de minimizar datos en logs.
- *Alternativa descartada 2*: mandarla por email. El adaptador de email llega recién en C-04, que depende de C-03.
- La contraseña de la variable es temporal: el cambio obligatorio (D8) la invalida en el primer uso. El odontólogo la recibe por el mismo canal que el resto de las credenciales de la instancia.

### D3 — Sesión de servidor en memoria del contenedor
`HttpSession` de Tomcat, sin Spring Session. `server.servlet.session.timeout=30m` y cookie `http-only`, `secure` y `same-site=strict` por configuración. La seguridad se arma con un único `SecurityFilterChain` en `identidad/infrastructure`; ninguna otra clase toca la configuración de Spring Security.
- *Alternativa descartada*: Spring Session JDBC. Sobrevive a reinicios y a varias réplicas, pero agrega dos tablas y escrituras por petición para un caso que DD-01 no tiene. Un reinicio cierra las sesiones: aceptable para un consultorio.
- *Alternativa descartada*: JWT. Ya descartado por DD-08.

### D4 — Login propio en JSON, no `formLogin`
`AuthController` (`identidad/api`) recibe `{email, password}` validado con Bean Validation y llama a `ServicioDeLogin` (`identidad/application`). Si el resultado es exitoso, el controlador crea el `SecurityContext`, rota el id de sesión (`request.changeSessionId()`) y lo guarda con `HttpSessionSecurityContextRepository`. Así el cuerpo de la respuesta y los errores siguen el formato del resto de la API.
- *Alternativa descartada*: `formLogin()` con handlers personalizados. Espera `application/x-www-form-urlencoded` y redirecciones; adaptarlo a JSON cuesta más que escribir el endpoint.

### D5 — El login fallido es un resultado, no una excepción
`ServicioDeLogin.autenticar` es `@Transactional` y devuelve un `ResultadoLogin` sellado (`Exitoso` | `Rechazado(motivo)`), sin lanzar excepción en un fallo. Dentro de la misma transacción:
1. Bloquea la fila con `SELECT … FOR UPDATE` (dos intentos simultáneos no pueden pisar el contador).
2. Si la cuenta está bloqueada (`bloqueado_hasta > ahora`) o inactiva, rechaza sin evaluar la contraseña.
3. Si el email no existe, compara contra un hash BCrypt fijo para que el tiempo de respuesta no revele si la cuenta existe.
4. Actualiza contadores y publica `EventoAuditoria`.

Como la transacción **se confirma** también en el fallo, el contador y el evento `LOGIN_FALLIDO` persisten juntos. Si el servicio lanzara una excepción, la transacción se revertiría y, por el diseño `BEFORE_COMMIT` de C-02, se perdería la auditoría del intento.

**Ventana de bloqueo** (con el `Clock` inyectado): si `primer_fallo_at` es nulo o más viejo que 15 min, el contador vuelve a 1 y `primer_fallo_at = ahora`; si no, incrementa. Al llegar a 5, `bloqueado_hasta = ahora + 15 min` y se limpia la ventana. Un login exitoso pone todo en cero.

### D6 — Errores de seguridad en RFC 7807 desde los filtros
Los rechazos que ocurren en filtros (sin sesión, CSRF, rol, rate limit) no pasan por `@RestControllerAdvice`. Un `AuthenticationEntryPoint`, un `AccessDeniedHandler` y el filtro de rate limit escriben `ProblemDetail` con el mismo `ObjectMapper`. Los `type` son URIs estables:

| Caso | Estado | `type` |
|---|---|---|
| Sin sesión o sesión vencida | 401 | `/problemas/no-autenticado` |
| Login rechazado (cualquier motivo) | 401 | `/problemas/credenciales-invalidas` |
| Rol insuficiente o CSRF inválido | 403 | `/problemas/acceso-denegado` |
| Cambio de contraseña pendiente | 403 | `/problemas/cambio-password-requerido` |
| Rate limit | 429 | `/problemas/demasiadas-peticiones` |

### D7 — CSRF con cookie legible y cabecera
`CookieCsrfTokenRepository.withHttpOnlyFalse()` con la cookie `XSRF-TOKEN` en `SameSite=Strict` y `CsrfTokenRequestAttributeHandler`, sin la máscara XOR, porque la SPA reenvía el valor crudo de la cookie en `X-XSRF-TOKEN`. `GET /api/v1/auth/csrf` fuerza la emisión del token antes del primer `POST`. El login **sí** exige CSRF, para evitar el ataque de "login CSRF". `/api/v1/webhooks/**` queda exento: lo protege la firma de Meta.

### D8 — Cambio de contraseña obligatorio con un filtro
Si `debe_cambiar_password` es verdadero, el principal recibe solo la autoridad `CAMBIO_PASSWORD_PENDIENTE`, sin `ROLE_*`. Un filtro posterior a la autenticación responde `403 /problemas/cambio-password-requerido` a cualquier ruta autenticada que no sea `GET /auth/me`, `POST /auth/logout` o `POST /auth/password`. Al cambiar la contraseña se reemplaza el `SecurityContext` de la sesión por uno con su `ROLE_*` y se rota el id de sesión.
- *Alternativa descartada*: resolverlo solo con `@PreAuthorize`. Un rol faltante daría un `403` genérico y la SPA no podría distinguir "rol insuficiente" de "debe cambiar la contraseña".

**Política de contraseña** (NIST SP 800-63B): de 10 a 72 **bytes** UTF-8 (BCrypt ignora lo que pasa de 72 bytes), distinta de la actual, sin reglas de composición.

### D9 — RBAC con anotaciones y constantes públicas
`@EnableMethodSecurity`. `identidad/application/Permisos` expone constantes SpEL de tiempo de compilación, por ejemplo `SOLO_ODONTOLOGO = "hasRole('ODONTOLOGO')"` y `PERSONAL = "hasAnyRole('ODONTOLOGO','SECRETARIA')"`. Los módulos las usan como `@PreAuthorize(Permisos.SOLO_ODONTOLOGO)` dependiendo solo de la capa `application` de `identidad`, que es su API pública y ArchUnit lo permite. En C-03 no hay rutas de historia clínica todavía: el `403` de RN-SEG-02 se prueba con un controlador de test anotado con `SOLO_ODONTOLOGO`.

### D10 — Rate limiting con Bucket4j en memoria
Un `OncePerRequestFilter` anterior a la cadena de seguridad aplica a `POST /api/v1/auth/login` y a `/api/v1/public/**`. Usa un bucket por IP (`request.getRemoteAddr()`) guardado en un `ConcurrentMap` acotado con expiración por inactividad, para que IPs nuevas no hagan crecer la memoria sin límite. Valores por defecto: `RATE_LIMIT_LOGIN_POR_MINUTO=10` y `RATE_LIMIT_PUBLICO_POR_MINUTO=60`. Al exceder el cupo responde `429` con `Retry-After` en segundos.
- *Alternativa descartada*: rate limit en un proxy (nginx o Caddy). Todavía no hay proxy (C-35), y el límite tiene que funcionar también en desarrollo y en los tests.
- El bloqueo por cuenta (D5) y el rate limit por IP se complementan: el primero frena el ataque a una cuenta, el segundo el barrido de muchas cuentas desde una IP.

### D11 — Eventos de auditoría

| Caso | `accion` | `actor_tipo` / `actor_id` | `entidad` / `entidad_id` | `detalle` |
|---|---|---|---|---|
| Login exitoso | `LOGIN` | `USUARIO` / id | `USUARIO` / id | `{}` |
| Contraseña incorrecta | `LOGIN_FALLIDO` | `SISTEMA` / null | `USUARIO` / id | `{"motivo_codigo":"CREDENCIALES"}` |
| Email inexistente | `LOGIN_FALLIDO` | `SISTEMA` / null | `USUARIO` / `desconocido` | `{"motivo_codigo":"CREDENCIALES"}` |
| Cuenta bloqueada o inactiva | `LOGIN_FALLIDO` | `SISTEMA` / null | `USUARIO` / id | `{"motivo_codigo":"BLOQUEADA"}` o `"INACTIVA"` |
| Cambio de contraseña | `MODIFICAR_ENTIDAD` | `USUARIO` / id | `USUARIO` / id | `{"referencia":"cambio-password"}` |

El actor de un fallo es `SISTEMA` porque nadie quedó autenticado. El email nunca se guarda: el `ValidadorDetalle` de C-02 lo rechazaría de todos modos.

### D12 — Cabeceras de seguridad
Con la configuración de cabeceras de Spring Security:
- `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'`
- `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`
- HSTS: `max-age=31536000; includeSubDomains`, que Spring Security solo emite sobre HTTPS.

### D13 — `401` antes que `404` en rutas desconocidas
Con la regla "todo `/api/v1/**` autenticado salvo la lista pública", Spring Security rechaza antes de que el dispatcher sepa si la ruta existe. Es el comportamiento deseado (no revelar la superficie de la API) y modifica el requisito de `plataforma-base` (ver delta).

### D14 — Tests
- Los tests de integración usan Testcontainers con PostgreSQL 16 real y `spring-security-test` (`@WithMockUser`, `csrf()`), y fijan el `Clock` para bloqueo y ventana.
- La expiración de 30 min se prueba verificando el `maxInactiveInterval` de la sesión creada. Esperar 30 min reales no es viable, y el vencimiento lo hace Tomcat.
- Los permisos del rol runtime sobre `usuario` se prueban con el `DataSource` runtime que ya usa C-02.
- Los tests existentes de C-01 que llaman a `/api/v1` sin sesión (`ManejadorGlobalDeErroresTest`, `ControladorDePrueba`) pasan a usar `@WithMockUser` y `csrf()`.

## Risks / Trade-offs

- [La CSP estricta sin `'unsafe-inline'` en `style-src` puede romper estilos inline que React o librerías inyecten] → Verificar la SPA de C-01 con la CSP activa. Si algo se rompe, ajustar en C-06 con hashes o nonces, no con `'unsafe-inline'` global.
- [La sesión en memoria se pierde al reiniciar o redesplegar] → Aceptado por DD-01: el usuario vuelve a iniciar sesión. Si alguna vez hay réplicas, migrar a Spring Session JDBC sin cambiar la API.
- [El rate limit por IP del socket ve una sola IP detrás de un proxy] → C-35 define el proxy y la cabecera de confianza. Hasta entonces el despliegue de demo expone la app sin proxy intermedio.
- [La contraseña inicial vive en una variable de entorno] → Es temporal y de un solo uso: el cambio obligatorio la invalida en el primer login. `.env.example` la deja vacía.
- [El índice parcial no impide quedarse sin odontólogo activo] → C-05 impide desactivar al odontólogo; un test de C-05 lo cubrirá.
- [Comparar contra un hash fijo para emails inexistentes cuesta ~250 ms de CPU por intento] → Lo acota el rate limit por IP.

## Migration Plan

1. `V3__identidad.sql` se aplica con el migrator (servicio `migrate` en compose, Flyway en los tests). Es aditiva: no toca `V1` ni `V2`.
2. Antes del primer arranque de una instancia nueva hay que definir `ADMIN_INITIAL_EMAIL` y `ADMIN_INITIAL_PASSWORD`. En instancias con odontólogo existente no hacen falta.
3. **Rollback**: revertir el despliegue de la imagen. La tabla `usuario` puede quedar, porque la versión anterior no la usa. Si hubiera que eliminarla, se hace con una migración nueva, nunca editando `V3`.

## Open Questions

- Los valores por defecto del rate limit (10 logins/min y 60 públicas/min por IP) se ajustarán con uso real; cambiarlos no requiere tocar código.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI) — 2026-11-25. Las pantallas de login y de cambio de contraseña son de C-06 y requieren su propio mockup aprobado.
