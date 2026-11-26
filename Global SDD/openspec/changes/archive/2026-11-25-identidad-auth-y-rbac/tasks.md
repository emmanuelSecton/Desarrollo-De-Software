# Tasks: identidad-auth-y-rbac (C-03)

Convenciones de apply:
- **Governance CRÍTICO**: no se empieza ninguna tarea sin la aprobación explícita del usuario sobre proposal, specs y design.
- **TDD estricto**: RED (test que falla) → GREEN → TRIANGULAR (caso feliz + borde) → REFACTOR, con los tests en verde tras cada paso.
- **Base real**: los tests de base usan Testcontainers con PostgreSQL 16, nunca H2 ni mocks de la base.
- **Datos ficticios**: emails `*@consultorio.test` y contraseñas de prueba generadas en el test.

## 1. Safety net y dependencias

- [x] 1.1 Ejecutar `./gradlew build` sobre `main` antes de tocar nada y registrar "N/N tests verdes"; si algo falla, frenar y reportar sin arreglarlo. Verificación: el baseline queda anotado en el resumen de apply.
- [x] 1.2 Agregar `spring-boot-starter-security`, `spring-security-test` (test) y `com.bucket4j:bucket4j_jdk17-core` con versión fija. Adaptar `ManejadorGlobalDeErroresTest`, `SpaForwardControllerTest`, `SaludTest` y los tests que usan `ControladorDePrueba` a la API cerrada por defecto (`@WithMockUser` y `csrf()` donde corresponda). Verificación: `./gradlew build` vuelve a quedar en verde con el mismo número de tests más los adaptados.

## 2. Migración `usuario`

- [x] 2.1 Crear `V3__identidad.sql` con la tabla de D1, el índice único parcial `uq_usuario_odontologo_activo` y `GRANT SELECT, INSERT, UPDATE ON usuario TO ${runtime_role}`; no tocar `V1` ni `V2`. Verificación: `EsquemaUsuarioTest` comprueba columnas y tipos (`CITEXT`, `TIMESTAMPTZ`), la ausencia de `DEFAULT now()` en `creado_at`, el `CHECK` de rol y el índice parcial.
- [x] 2.2 Probar las garantías de base. Verificación: `RestriccionesUsuarioTest` rechaza un email duplicado con otra capitalización, un segundo `ODONTOLOGO` activo y un rol fuera del `CHECK`, y acepta un segundo `ODONTOLOGO` inactivo y varias `SECRETARIA`.
- [x] 2.3 Probar los permisos del rol runtime con el `DataSource` runtime de C-02. Verificación: `PermisosUsuarioRuntimeTest` permite `SELECT`, `INSERT` y `UPDATE` y rechaza `DELETE` sobre `usuario`.
- [x] 2.4 Actualizar `MigracionBaselineTest` para esperar `["1:baseline", "2:auditoria", "3:identidad"]`. Verificación: el test queda verde y un segundo arranque no reaplica migraciones.

## 3. Dominio, persistencia y alta inicial

- [x] 3.1 Modelar `Usuario` y `Rol` en `identidad/domain` con la lógica de ventana y bloqueo de D5 (`registrarFallo(ahora)`, `estaBloqueada(ahora)`, `registrarExito(ahora)`) sin dependencias de Spring. Verificación: `UsuarioTest` unitario con instantes fijos cubre el 4.º y el 5.º fallo dentro de la ventana, fallos fuera de la ventana, el bloqueo vigente a los 10 min, el bloqueo vencido a los 15 min y el reinicio tras un éxito.
- [x] 3.2 Implementar el puerto `RepositorioUsuarios` en `application` y su adaptador JDBC en `infrastructure`, con búsqueda por email y `SELECT … FOR UPDATE`. Verificación: `RepositorioUsuariosTest` con Testcontainers guarda, busca sin distinguir mayúsculas y persiste los contadores.
- [x] 3.3 Implementar el `ApplicationRunner` de alta inicial (D2) con `ADMIN_INITIAL_EMAIL` y `ADMIN_INITIAL_PASSWORD` y documentar ambas variables vacías en `.env.example` y en `deploy/docker-compose.yml`. Verificación: `AltaOdontologoInicialTest` cubre la base vacía con variables (crea el usuario con hash BCrypt cost 12 y cambio obligatorio), la base con odontólogo (no cambia nada) y la base vacía sin variables (falla nombrando la variable). Un appender de logback de test verifica que ningún log contiene el email ni la contraseña.

## 4. Login, sesión y auditoría

- [x] 4.1 Implementar `ServicioDeLogin` (`@Transactional`, devuelve `ResultadoLogin` sin lanzar excepción en un fallo, compara contra un hash fijo si el email no existe) y la publicación de `EventoAuditoria` según D11. Verificación: `ServicioDeLoginTest` con Testcontainers y `Clock` fijo cubre el éxito (`LOGIN`, contador en cero, `ultimo_login_at`), la contraseña incorrecta, el email inexistente, el usuario inactivo y la cuenta bloqueada. Para cada `LOGIN_FALLIDO`, el contador y el evento quedan persistidos y ninguna columna de `evento_auditoria` contiene el email.
- [x] 4.2 Configurar el `SecurityFilterChain` (D3, D6, D7, D13): rutas públicas, todo `/api/v1/**` autenticado, `AuthenticationEntryPoint` y `AccessDeniedHandler` en RFC 7807, CSRF por cookie (webhooks exentos), cookie de sesión `HttpOnly`/`Secure`/`SameSite=Strict` y timeout de 30 min. Verificación: `ReglasDeAccesoTest` (MockMvc) devuelve `401` problem+json sin sesión en una ruta de negocio y en `/api/v1/no-existe`, `404` con sesión en `/api/v1/no-existe`, y deja accesibles sin sesión `/actuator/health`, `/` y `/api/v1/auth/csrf`.
- [x] 4.3 Implementar `GET /auth/csrf`, `POST /auth/login`, `POST /auth/logout` y `GET /auth/me` en `AuthController` (D4), con rotación del id de sesión. Verificación: `AuthControllerTest` cubre el login `200` con datos y rol, la cookie nueva distinta de la anónima previa, el `401` con cuerpo idéntico para email inexistente y para contraseña incorrecta, el `403` sin `X-XSRF-TOKEN`, el logout `204` con `401` al reusar la cookie, `me` con sesión y `me` sin sesión. Un test verifica los atributos de la cookie y el `maxInactiveInterval` de 1800 s.
- [x] 4.4 Prueba de punta a punta del bloqueo por HTTP. Verificación: `BloqueoPorIntentosTest` hace 5 `POST /auth/login` fallidos y luego uno correcto que responde `401`; tras avanzar el `Clock` 15 min, el correcto responde `200`.

## 5. Cambio de contraseña obligatorio

- [x] 5.1 Implementar `POST /api/v1/auth/password` con la política de D8 (de 10 a 72 bytes UTF-8, distinta de la actual, exige la actual), la auditoría `MODIFICAR_ENTIDAD` y el reemplazo del `SecurityContext` con rotación de sesión. Verificación: `CambioPasswordTest` cubre el cambio exitoso (`204`, marca limpia, nuevo hash), la actual incorrecta (`400`), la nueva igual a la actual (`400`), la de 9 caracteres (`400`) y la de más de 72 bytes con caracteres multibyte (`400`).
- [x] 5.2 Implementar el filtro de cambio pendiente. Verificación: `CambioPasswordPendienteTest` devuelve `403` con `type` `/problemas/cambio-password-requerido` en una ruta de negocio, `200` en `me`, `204` en logout y deja acceder a la ruta de negocio después del cambio.

## 6. RBAC

- [x] 6.1 Habilitar `@EnableMethodSecurity` y crear `identidad.application.Permisos` con las constantes `SOLO_ODONTOLOGO` y `PERSONAL`. Verificación: `AutorizacionPorRolTest`, con un controlador de test anotado `@PreAuthorize(Permisos.SOLO_ODONTOLOGO)` que simula una ruta de historia clínica, devuelve `403` problem+json a la `SECRETARIA` sin ejecutar el método y `200` al `ODONTOLOGO`. `ArquitecturaTest` sigue verde.

## 7. Rate limiting y cabeceras

- [x] 7.1 Implementar el filtro Bucket4j de D10 con mapa acotado y variables `RATE_LIMIT_LOGIN_POR_MINUTO` y `RATE_LIMIT_PUBLICO_POR_MINUTO` (documentadas en `.env.example`). Verificación: `RateLimitTest` con cupo 3 responde `429` problem+json con `Retry-After` en el 4.º login desde una IP, procesa normalmente el login desde otra IP y aplica el límite a `/api/v1/public/**`. En el `429` no se evalúan las credenciales: no se genera ningún evento `LOGIN_FALLIDO`.
- [x] 7.2 Configurar las cabeceras de D12. Verificación: `CabecerasDeSeguridadTest` encuentra CSP, `X-Frame-Options: DENY`, `nosniff` y `Referrer-Policy` en `/`, `/api/v1/auth/csrf` y una respuesta `401`; HSTS solo en una petición `https` simulada.

## 8. Verificación integral

- [x] 8.1 Ejecutar `./gradlew build` completo: todos los tests en verde, JaCoCo ≥ 80 % de líneas y ArchUnit sin violaciones. Verificación: salida del build con totales de tests y cobertura en el resumen de apply.
- [x] 8.2 Levantar el stack con `docker compose` (servicio `migrate` + app) en una base vacía y recorrer el flujo real: alta inicial, `csrf`, login, `403` por cambio pendiente, cambio de contraseña, `me` y logout. Verificación: la secuencia de `curl` y sus códigos de estado anotados en el resumen de apply; la SPA de C-01 carga sin errores de CSP en la consola del navegador.

## Workflow follow-up

- Revisión humana del PR antes de mergear (governance CRÍTICO).
- Marcar C-03 como `[x]` en `CHANGES.md`, sincronizar los deltas a `openspec/specs/identidad` y `openspec/specs/plataforma-base`, y archivar el change.
