# Design — pacientes-core (C-09)

## Context

- El módulo `pacientes` existe vacío (solo `package-info.java` por capa). ArchUnit permite que otros módulos usen su capa `application` y prohíbe que usen `api`, `domain` o `infrastructure`.
- C-02 dejó la auditoría como eventos `EventoAuditoria` escritos en `BEFORE_COMMIT` de la misma transacción; el `detalle` solo admite claves de una whitelist (`paciente_id` está incluida) y rechaza `dni`, `nombre`, `telefono`, `email`.
- C-03 dejó la API cerrada por defecto, `@PreAuthorize` con `Permisos.PERSONAL` y `Permisos.SOLO_ODONTOLOGO`, y como principal de la sesión el `UUID` del usuario.
- La persistencia de `identidad` y `auditoria` es JDBC con `NamedParameterJdbcTemplate` perezoso; no hay entidades JPA.
- `V1` habilitó `btree_gist`, `citext` y `pgcrypto`; no hay `unaccent`.
- Volumen esperado: un consultorio con unos pocos miles de pacientes.

## Goals / Non-Goals

**Goals:**
- Que el DNI sea la identidad del paciente en todo el sistema y que la base garantice su unicidad.
- Que el teléfono guardado sirva directo para WhatsApp (E.164 de celular).
- Que C-10 y C-20 puedan reusar el alta sin duplicar reglas ni pisar datos.

**Non-Goals:**
- Búsqueda difusa por similitud (trigramas) o indexada por texto completo: con este volumen alcanza un barrido.
- Bloqueo optimista en `PUT`: dos ediciones simultáneas de un mismo paciente son improbables en un consultorio; gana la última.
- Números extranjeros como caso principal: se aceptan si vienen con `+código de país`.

## Decisions

### D1 — Tabla `paciente`
`V4__pacientes.sql` crea:

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `UUID PK DEFAULT gen_random_uuid()` | |
| `dni` | `VARCHAR(10) NOT NULL UNIQUE` | `CHECK (dni ~ '^[0-9]{7,9}$')` |
| `nombre`, `apellido` | `VARCHAR(80) NOT NULL` | |
| `fecha_nacimiento` | `DATE` | opcional |
| `telefono_e164` | `VARCHAR(16) NOT NULL` | `CHECK (telefono_e164 ~ '^\+[1-9][0-9]{7,14}$')` |
| `email` | `CITEXT` | opcional |
| `consentimiento_datos_at` | `TIMESTAMPTZ NOT NULL` | RN-DP-01 |
| `politica_privacidad_version` | `VARCHAR(20) NOT NULL` | versión del texto aceptado (12_cumplimiento_legal.md) |
| `acepta_whatsapp` | `BOOLEAN NOT NULL DEFAULT FALSE` | |
| `whatsapp_consentimiento_at` | `TIMESTAMPTZ` | `CHECK (acepta_whatsapp = (whatsapp_consentimiento_at IS NOT NULL))` |
| `ausencias_count` | `INT NOT NULL DEFAULT 0` | `CHECK (ausencias_count >= 0)`; lo incrementa C-10 |
| `activo` | `BOOLEAN NOT NULL DEFAULT TRUE` | baja lógica |
| `creado_at`, `actualizado_at` | `TIMESTAMPTZ NOT NULL` | del `Clock`, sin `DEFAULT now()` |

- Índices: el único de `dni`, `(apellido, nombre)` y `telefono_e164`.
- Grants: `SELECT, INSERT, UPDATE ON paciente TO ${runtime_role}`; sin `DELETE`.
- `CREATE EXTENSION IF NOT EXISTS unaccent` (extensión *trusted* desde PostgreSQL 13: la crea el migrator, dueño de la base).
- Dos columnas que la KB no tenía: `politica_privacidad_version` (el requisito legal pide texto versionado) y `whatsapp_consentimiento_at` (RN-DP-01 pide la fecha también para WhatsApp).
- *Alternativa descartada*: `dni` como clave primaria. Corregir un DNI mal cargado obligaría a actualizar todas las claves foráneas de turnos, historia y cobros.

### D2 — Persistencia JDBC, igual que identidad
`RepositorioPacientesJdbc` en `infrastructure` implementa el puerto `RepositorioPacientes` de `application`, con `ObjectProvider<NamedParameterJdbcTemplate>` perezoso.
- *Alternativa descartada*: JPA. El resto del backend ya es JDBC y mezclar estilos complica los tests y las consultas con `unaccent`.

### D3 — Teléfonos con libphonenumber y regla de celular argentino
`NormalizadorDeTelefono` (`application`) usa `libphonenumber` con región por defecto `AR`:
1. Parsea el texto; acepta espacios, guiones, paréntesis, `0` de larga distancia y el `15` de celular.
2. Si el país es Argentina (`+54`) y el número nacional no empieza con `9`, le antepone el `9`. En Argentina los celulares y los fijos comparten numeración y solo el `9` (o el `15`) los distingue; como el campo es "celular", un número de 10 dígitos sin marca se interpreta como celular. Así `1123456789` queda `+5491123456789`, el formato que exige WhatsApp.
3. Exige que el resultado sea válido y de tipo celular (`MOBILE` o `FIXED_LINE_OR_MOBILE`); si no, `400`.
- *Alternativa descartada*: una expresión regular propia. Los códigos de área argentinos tienen 2, 3 o 4 dígitos y el `15` cambia de lugar; libphonenumber ya resuelve eso con metadatos mantenidos.

### D4 — API y DTOs
`PacientesController` (`api`) con DTOs separados de la entidad:
- `AltaPaciente` y `CambioPaciente`: Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@PastOrPresent`, `@AssertTrue` para `consentimientoDatos` en el alta).
- `PacienteRespuesta`: id, dni, nombre, apellido, fechaNacimiento, telefono, email, aceptaWhatsapp, ausenciasCount, activo, consentimientoDatosAt.
- Búsqueda paginada: `pagina` (desde 0) y `tamano` (por defecto 20, máximo 100); respuesta `{contenido, pagina, tamano, total}`.
- `409` como `ProblemDetail` con `type` `/problemas/paciente-duplicado` y la propiedad `pacienteId`; `404` con `type` `/problemas/paciente-inexistente`.

### D5 — Búsqueda
Según la forma de `q` (sin espacios ni puntos):
- **Solo dígitos, 7 a 9**: coincide por DNI exacto **o** por teléfono que termine en esos dígitos.
- **Teléfono normalizable** (por ejemplo con `+`, `15` o 10 dígitos): coincide por `telefono_e164` exacto.
- **Texto**: cada palabra debe aparecer en `unaccent(lower(nombre || ' ' || apellido))`; así `ana perez` encuentra "Ana Pérez" y "Pérez Ana".
- **Vacío**: lista los activos.

Orden `apellido, nombre, id`. Con unos miles de filas, un barrido con `unaccent` cuesta milisegundos; no se agrega índice de expresión porque `unaccent` no es `IMMUTABLE`.

### D6 — Unicidad de DNI y concurrencia
- `POST` busca el DNI primero para responder `409` con el id existente. Si dos altas compiten, la segunda choca con el `UNIQUE` y el `DuplicateKeyException` se traduce también a `409`.
- **Obtener o crear** (`RegistroDePacientes.obtenerOCrear`, capa `application`, pública para otros módulos): `INSERT … ON CONFLICT (dni) DO NOTHING RETURNING id`. Si no inserta, lee el existente y lo devuelve sin tocarlo. Las dos llamadas concurrentes terminan con el mismo id y solo la que insertó audita el alta.

### D7 — Consentimiento
- El alta exige `consentimientoDatos = true`; `consentimiento_datos_at` se toma del `Clock` y `politica_privacidad_version` de la configuración `pacientes.politica-privacidad-version` (variable `POLITICA_PRIVACIDAD_VERSION`, por defecto `v0-provisoria` mientras los textos legales estén en `(TBD)`).
- `aceptaWhatsapp`: de `false` a `true` fija `whatsapp_consentimiento_at = ahora`; de `true` a `false` la limpia; sin cambio, la conserva.
- El `PUT` no modifica `consentimiento_datos_at`, la versión ni `ausencias_count`.

### D8 — Permisos
`@PreAuthorize(Permisos.PERSONAL)` en búsqueda, alta, consulta y modificación; `@PreAuthorize(Permisos.SOLO_ODONTOLOGO)` en baja y reactivación. Al ser `pacientes` un módulo distinto, solo usa `identidad.application.Permisos`.

### D9 — Auditoría
| Operación | `accion` | Actor | `detalle` |
|---|---|---|---|
| Alta (interna) | `CREAR_ENTIDAD` | `USUARIO` / id de la sesión | `{"paciente_id": id}` |
| Modificación | `MODIFICAR_ENTIDAD` | `USUARIO` | `{"paciente_id": id}` |
| Baja | `ANULAR_ENTIDAD` | `USUARIO` | `{"paciente_id": id}` |
| Reactivación | `MODIFICAR_ENTIDAD` | `USUARIO` | `{"paciente_id": id, "referencia": "reactivacion"}` |
| Obtener o crear (si crea) | `CREAR_ENTIDAD` | el que indique el módulo que llama | `{"paciente_id": id}` |

Siempre con `entidad = 'PACIENTE'` y `entidad_id = id`. El actor de la sesión se obtiene del `UUID` principal que dejó C-03; la operación "obtener o crear" recibe el actor como parámetro porque C-20 actuará como `PACIENTE`.

### D10 — Tests
- Testcontainers con PostgreSQL 16 real para esquema, permisos del rol runtime, búsqueda con acentos y la concurrencia de "obtener o crear" (dos hilos con un `CountDownLatch`).
- Tests unitarios del normalizador con los formatos argentinos habituales y casos inválidos.
- MockMvc con la cadena de seguridad real para permisos y respuestas `400/404/409`.

## Risks / Trade-offs

- [La regla del `9` convierte en "celular" un fijo argentino cargado sin `15`] → El campo se presenta como celular; un fijo no sirve para WhatsApp de todas formas. Se documenta en el formulario (C-08).
- [Cualquiera que conozca un DNI ve que "ya existe" al reservar] → Lo decide C-20 (DD-05, PA-05); "obtener o crear" nunca devuelve datos a quien llama desde lo público.
- [La búsqueda con `unaccent` no usa índice] → Aceptable para el volumen de un consultorio; si crece, se agrega una columna normalizada o `pg_trgm`.
- [La versión de la política es provisoria] → Se cambia por variable de entorno sin tocar código cuando estén los textos legales (12_cumplimiento_legal.md).

## Migration Plan

1. `V4__pacientes.sql` se aplica con el migrator (servicio `migrate` en compose, Flyway en tests). Es aditiva.
2. Nueva variable opcional `POLITICA_PRIVACIDAD_VERSION`, documentada en compose y README (y a agregar en `.env.example`).
3. **Rollback**: revertir la imagen; la tabla puede quedar. Cualquier corrección del esquema va en una migración nueva.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI) — 2026-11-26. Las pantallas de búsqueda y alta de pacientes son de C-08 y C-26, con su propio mockup.
