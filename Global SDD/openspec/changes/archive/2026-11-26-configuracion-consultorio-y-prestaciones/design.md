# Design — configuracion-consultorio-y-prestaciones (C-05)

## Context

- El backend tiene los módulos `agenda`, `pacientes`, `clinica`, `caja`, `notificaciones`, `integraciones`, `identidad` y `auditoria`; no existe uno de configuración. El módulo `agenda` está vacío.
- C-01 tomó la zona horaria del consultorio de `APP_TIMEZONE` (`ZonaHorariaConsultorio`), aunque la KB también la lista como columna `consultorio.zona_horaria`.
- C-03 dejó `Permisos.PERSONAL` y `Permisos.SOLO_ODONTOLOGO`; C-09 dejó el patrón de módulo con persistencia JDBC, DTOs, `ProblemDetail` propios del controlador y auditoría por eventos.
- `btree_gist` está habilitada desde `V1`.

## Goals / Non-Goals

**Goals:**
- Que la base garantice la coherencia de la configuración (singleton, franjas sin superposición, duraciones válidas), no solo la API.
- Que C-10, C-14 y C-20 lean la configuración por una API de `agenda.application` sin conocer las tablas.

**Non-Goals:**
- Excepciones de horario por fecha (feriados, vacaciones): son bloqueos de agenda (C-11).
- Varios profesionales por consultorio: v1 tiene uno solo (03_actores_y_roles.md).
- Bloqueo optimista en las ediciones: un solo odontólogo edita la configuración.

## Decisions

### D1 — Todo en el módulo `agenda`
Consultorio, horarios y prestaciones viven en `agenda` (api, application, domain, infrastructure). `agenda.application.ConfiguracionDeAgenda` expone consultas de solo lectura (parámetros del consultorio, franjas por día, prestación por id) para los changes siguientes.
- *Alternativa descartada*: un módulo nuevo `configuracion`. Habría que sumarlo a ArchUnit, y la agenda, su principal consumidor, dependería de él.

### D2 — La zona horaria sigue en `APP_TIMEZONE`
No se agrega `consultorio.zona_horaria`. La zona ya viene de la variable de entorno (requisito "Zona horaria del consultorio configurable" de `plataforma-base`), y tenerla en dos lugares abriría la puerta a que difieran. Se anota la diferencia con la KB.

### D3 — Tabla `consultorio` con singleton en la base
`V6__configuracion_consultorio.sql`:

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `UUID PK DEFAULT gen_random_uuid()` | |
| `unico` | `BOOLEAN NOT NULL DEFAULT TRUE` | `UNIQUE` + `CHECK (unico)`: como máximo una fila |
| `nombre`, `profesional_nombre` | `VARCHAR(120) NOT NULL` | provisorios en el seed |
| `matricula` | `VARCHAR(30)` | |
| `direccion` | `VARCHAR(200)` | |
| `telefono` | `VARCHAR(30)` | texto libre: es el del consultorio, no un celular para WhatsApp |
| `email` | `CITEXT` | |
| `slug_reserva` | `VARCHAR(60) NOT NULL UNIQUE` | `CHECK (slug_reserva ~ '^[a-z0-9-]{3,60}$')` |
| `recordatorio_horas_antes` | `INT NOT NULL DEFAULT 48` | `CHECK (BETWEEN 1 AND 168)` |
| `confirmacion_limite_horas` | `INT NOT NULL DEFAULT 24` | `CHECK (>= 1 AND < recordatorio_horas_antes)` |
| `cancelacion_minima_horas` | `INT NOT NULL DEFAULT 24` | `CHECK (BETWEEN 0 AND 168)` |
| `oferta_vencimiento_minutos` | `INT NOT NULL DEFAULT 30` | `CHECK (BETWEEN 5 AND 1440)` |
| `actualizado_at` | `TIMESTAMPTZ NOT NULL` | |

El rol runtime solo tiene `SELECT, UPDATE`: no puede crear ni borrar el consultorio.

### D4 — Franjas sin superposición con una exclusion constraint
`horario_atencion(id UUID, dia_semana SMALLINT CHECK (1..7), hora_desde TIME, hora_hasta TIME)` con:
- `CHECK (hora_desde < hora_hasta)`.
- `CHECK` de múltiplos de 5 minutos y segundos en cero para ambas horas.
- `EXCLUDE USING gist (dia_semana WITH =, tsrange(DATE '2000-01-01' + hora_desde, DATE '2000-01-01' + hora_hasta) WITH &&)`. Al sumar la hora a una fecha fija se obtiene un `timestamp` y un rango comparable. Como el rango es `[)`, las franjas contiguas (09:00–13:00 y 13:00–14:00) se aceptan.

Igual que con los turnos (regla dura del proyecto), la garantía la da la base: la violación se traduce a `409`.

### D5 — Prestaciones con baja lógica y nombre único entre activas
`prestacion(id UUID, nombre VARCHAR(80), duracion_minutos INT CHECK (> 0 AND % 5 = 0 AND <= 480), precio_referencia NUMERIC(12,2) CHECK (>= 0), reservable_online BOOLEAN, activa BOOLEAN)`.
- Índice único parcial `lower(nombre) WHERE activa`: no puede haber dos activas con el mismo nombre, pero una inactiva puede repetirlo.
- Sin `DELETE` para el runtime: C-10 guardará referencias a la prestación en los turnos.
- El tope de 480 minutos (8 h) evita errores de carga; no está en la KB y se anota.

### D6 — Datos iniciales en la migración
El mismo `V6` inserta el consultorio (`nombre = 'Mi consultorio'`, `profesional_nombre = 'Profesional (completar)'`, `slug_reserva = 'mi-consultorio'`), las 10 franjas de lunes a viernes y las 6 prestaciones de la KB (`reservable_online = true`, sin precio).
- Flyway lo ejecuta **una sola vez por base**, así que si el odontólogo borra una franja de ejemplo no reaparece al reiniciar.
- *Alternativa descartada*: un `ApplicationRunner` como el del odontólogo inicial. Tendría que distinguir "nunca sembrado" de "sembrado y editado", y la migración ya resuelve eso.

### D7 — API
- `ConsultorioController`, `HorariosController` y `PrestacionesController` (`agenda.api`) con DTOs y Bean Validation, incluida una validación cruzada (`@AssertTrue`) de confirmación menor que recordatorio.
- `GET` con `@PreAuthorize(Permisos.PERSONAL)` y escrituras con `@PreAuthorize(Permisos.SOLO_ODONTOLOGO)`.
- `409` como `ProblemDetail` con `type` `/problemas/horario-superpuesto` y `/problemas/prestacion-duplicada`; `404` con `/problemas/no-encontrado`. La exclusion constraint llega como `DataIntegrityViolationException` con SQLState `23P01`, y el índice único como `23505`.
- Las horas viajan como `"HH:mm"` (`LocalTime`).

### D8 — Auditoría
`MODIFICAR_ENTIDAD` para el consultorio; `CREAR_ENTIDAD`, `MODIFICAR_ENTIDAD` y `ANULAR_ENTIDAD` para horarios y prestaciones, también para el borrado físico de una franja. Actor `USUARIO` con el id de la sesión, entidad `CONSULTORIO`, `HORARIO_ATENCION` o `PRESTACION`, y el id del registro; `detalle` vacío.

### D9 — Tests
- Testcontainers para el singleton, los `CHECK`, la exclusion constraint (superpuesta, contigua y otro día) y los permisos del rol runtime.
- Los tests que modifican el consultorio o las franjas iniciales restauran los valores al terminar, porque la base es compartida por toda la suite.
- MockMvc con la seguridad real para la API y los permisos.

## Risks / Trade-offs

- [Los valores provisorios del seed llegan a la reserva pública si nadie los cambia] → C-08 abre el asistente en el primer login; C-20 puede exigir que el nombre ya no sea el provisorio.
- [`slug_reserva` hoy no tiene uso] → Lo usará C-20 para la URL pública; se valida desde ya para no migrar datos después.
- [Borrar franjas no revisa turnos futuros] → En C-05 todavía no hay turnos. C-10 deberá decidir si un cambio de horario afecta turnos ya dados.

## Migration Plan

1. `V6__configuracion_consultorio.sql` se aplica con el migrator. Es aditiva e incluye los datos iniciales.
2. **Rollback**: revertir la imagen; las tablas pueden quedar. Cualquier corrección va en una migración nueva.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI) — 2026-11-26. El asistente de configuración y el ABM de horarios y prestaciones son de C-08, con su propio mockup.
