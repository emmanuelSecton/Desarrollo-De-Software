# Modelo de Datos

Convenciones: PostgreSQL, claves `UUID`, timestamps `TIMESTAMPTZ` en UTC (se muestran en `America/Argentina/Buenos_Aires`), migraciones con Flyway. Cada instancia pertenece a **un** consultorio, así que no hay columna `tenant_id` (DD-01).

## Dominios

| Dominio | Entidades |
|---|---|
| Identidad | `usuario` |
| Configuración | `consultorio`, `horario_atencion`, `prestacion` |
| Agenda | `turno`, `bloqueo_agenda`, `lista_espera`, `oferta_hueco`, `oferta_destinatario` |
| Pacientes | `paciente` |
| Historia clínica | `entrada_historia`, `odontograma_pieza` |
| Caja | `presupuesto`, `presupuesto_item`, `cobro` |
| Notificaciones | `notificacion` |
| Integraciones | `integracion_google` |
| Auditoría | `evento_auditoria` |

## ERD

```
consultorio 1──* horario_atencion
consultorio 1──* prestacion
usuario (ODONTOLOGO|SECRETARIA)

paciente 1──* turno *──1 prestacion
turno 1──* notificacion
paciente 1──* lista_espera *──0..1 prestacion
oferta_hueco 1──* oferta_destinatario *──1 lista_espera
oferta_hueco *──0..1 turno (turno_origen, cancelado) · 0..1 turno (turno_creado)
oferta_destinatario 1──* notificacion
paciente 1──* entrada_historia ──0..1 entrada_historia (enmienda_de)
entrada_historia 1──* odontograma_pieza            (solo tipo ODONTOGRAMA)
paciente 1──* presupuesto 1──* presupuesto_item *──1 prestacion
paciente 1──* cobro *──0..1 presupuesto
usuario 1──* entrada_historia (autor)
usuario 1──* evento_auditoria (actor, nulo si es el sistema o el paciente)
```

## Entidades

### consultorio
- `id UUID PK`, `nombre VARCHAR(120)`, `profesional_nombre VARCHAR(120)`, `matricula VARCHAR(30)`, `direccion`, `telefono`, `email`, `zona_horaria VARCHAR(40) DEFAULT 'America/Argentina/Buenos_Aires'`, `slug_reserva VARCHAR(60) UNIQUE`
- `recordatorio_horas_antes INT DEFAULT 48`, `confirmacion_limite_horas INT DEFAULT 24`, `cancelacion_minima_horas INT DEFAULT 24`, `oferta_vencimiento_minutos INT DEFAULT 30`
- Una sola fila por instancia (constraint de singleton).

### horario_atencion
- `id`, `dia_semana SMALLINT (1-7)`, `hora_desde TIME`, `hora_hasta TIME`
- Constraint: `hora_desde < hora_hasta`.

### prestacion
- `id`, `nombre VARCHAR(80)`, `duracion_minutos INT CHECK (> 0 AND % 5 = 0)`, `precio_referencia NUMERIC(12,2)`, `reservable_online BOOLEAN`, `activa BOOLEAN`

### usuario
- `id`, `email CITEXT UNIQUE`, `password_hash`, `nombre`, `rol ENUM('ODONTOLOGO','SECRETARIA')`, `activo BOOLEAN`, `ultimo_login TIMESTAMPTZ`
- Constraint: exactamente un `ODONTOLOGO` activo.

### paciente
- `id`, `dni VARCHAR(10) UNIQUE`, `nombre`, `apellido`, `fecha_nacimiento DATE`, `telefono_e164 VARCHAR(16)`, `email`, `acepta_whatsapp BOOLEAN`, `consentimiento_datos_at TIMESTAMPTZ`, `ausencias_count INT DEFAULT 0`, `creado_at`
- Índices: `dni`, `(apellido, nombre)`, `telefono_e164`.
- No se borra físicamente (RN-HC-03); `activo BOOLEAN` para ocultarlo.

### turno
- `id`, `paciente_id FK`, `prestacion_id FK`, `inicio TIMESTAMPTZ`, `fin TIMESTAMPTZ`
- `estado ENUM('RESERVADO','CONFIRMADO','CANCELADO','ATENDIDO','AUSENTE')`
- `origen ENUM('INTERNO','ONLINE')`, `sobreturno BOOLEAN DEFAULT false`, `reprogramado_de FK turno NULL`, `token_publico VARCHAR(64) UNIQUE` (enlace firmado), `sin_confirmar_alertado BOOLEAN`, `google_event_id`, `cancelado_por ENUM('PACIENTE','CONSULTORIO')`, `motivo_cancelacion`, `creado_at`, `actualizado_at`, `version INT` (optimistic locking)
- **Constraint anti-solapamiento**: `EXCLUDE USING gist (tstzrange(inicio, fin) WITH &&) WHERE (estado IN ('RESERVADO','CONFIRMADO') AND NOT sobreturno)` (RN-AG-01, RN-AG-07). Requiere la extensión `btree_gist`.
- Índices: `(inicio)`, `(estado, inicio)`.

### bloqueo_agenda
- `id`, `inicio`, `fin`, `motivo`, `creado_por FK usuario`
- La validación de no superponer con turnos activos se hace en el servicio (RN-AG-03).

### lista_espera
- `id`, `paciente_id FK`, `prestacion_id FK NULL`, `preferencia_texto VARCHAR(200)` (por ejemplo "martes por la tarde"), `estado ENUM('ACTIVA','ASIGNADA','DESCARTADA')`, `creado_at`

### oferta_hueco (DD-10)
- `id`, `inicio TIMESTAMPTZ`, `fin TIMESTAMPTZ`, `prestacion_id FK`, `turno_origen_id FK turno NULL` (el turno cancelado que liberó el hueco), `estado ENUM('ABIERTA','TOMADA','VENCIDA','CANCELADA')`, `vence_at TIMESTAMPTZ`, `turno_creado_id FK turno NULL`, `creada_por FK usuario`, `creado_at`, `cerrada_at`
- **Una sola oferta abierta por hueco**: `EXCLUDE USING gist (tstzrange(inicio, fin) WITH &&) WHERE (estado = 'ABIERTA')` (RN-LE-07).
- La disponibilidad pública (RN-AG-04) descuenta los rangos con oferta `ABIERTA`.
- Índice: `(estado, vence_at)` para el job de vencimiento.

### oferta_destinatario
- `id`, `oferta_id FK`, `lista_espera_id FK`, `paciente_id FK`, `estado ENUM('ENVIADA','ACEPTADA','TARDE','SIN_RESPUESTA')`, `respondido_at`, `token_respuesta VARCHAR(32) UNIQUE` (va en el payload del botón)
- Constraint: entre 1 y 5 destinatarios por oferta (validado en el servicio, RN-LE-04); `UNIQUE (oferta_id, paciente_id)`.

### entrada_historia (append-only)
- `id`, `paciente_id FK`, `tipo ENUM('ANAMNESIS','EVOLUCION','ODONTOGRAMA','ENMIENDA')`, `contenido JSONB`, `autor_id FK usuario`, `turno_id FK NULL`, `enmienda_de FK entrada_historia NULL`, `creado_at`, `hash_sha256 CHAR(64)`, `hash_anterior CHAR(64)`
- **Sin UPDATE ni DELETE**: lo impide un trigger y el usuario de base de la aplicación no tiene esos permisos (RN-HC-01).
- `hash_anterior` encadena las entradas del paciente para detectar alteraciones (RN-HC-02).

### odontograma_pieza
- `id`, `entrada_id FK`, `pieza SMALLINT` (numeración FDI 11-48 y 51-85), `cara ENUM('O','M','D','V','L','P','I','COMPLETA')`, `estado VARCHAR(30)` (caries, obturado, ausente, corona, endodoncia, a extraer, etc.), `realizado BOOLEAN` (realizado o a realizar)
- El odontograma vigente es la última entrada de tipo `ODONTOGRAMA`; las anteriores quedan como historial.

### presupuesto / presupuesto_item
- `presupuesto`: `id`, `paciente_id`, `estado ENUM('BORRADOR','ENTREGADO','ACEPTADO','RECHAZADO')`, `total NUMERIC(12,2)`, `creado_at`, `validez_dias`
- `presupuesto_item`: `id`, `presupuesto_id`, `prestacion_id`, `pieza SMALLINT NULL`, `descripcion`, `cantidad`, `precio_unitario`

### cobro
- `id`, `paciente_id`, `presupuesto_id NULL`, `monto NUMERIC(12,2) CHECK (> 0)`, `medio ENUM('EFECTIVO','TRANSFERENCIA','DEBITO','CREDITO','OTRO')`, `fecha`, `observacion`, `anulado BOOLEAN`, `anulado_motivo`, `registrado_por FK usuario`
- El saldo del paciente se calcula como presupuestos aceptados menos cobros no anulados.

### notificacion
- `id`, `turno_id FK NULL`, `oferta_destinatario_id FK NULL`, `canal ENUM('WHATSAPP','EMAIL')`, `tipo ENUM('RECORDATORIO','CONFIRMACION_RESERVA','CANCELACION','REPROGRAMACION','OFERTA_HUECO','OFERTA_TOMADA')`, `estado ENUM('PENDIENTE','ENVIADA','ENTREGADA','LEIDA','FALLIDA','RESPONDIDA')`, `proveedor_msg_id`, `respuesta VARCHAR(20)`, `error`, `programada_para`, `enviada_at`, `intentos SMALLINT`
- `CHECK (num_nonnulls(turno_id, oferta_destinatario_id) = 1)`: cada notificación es de un turno o de una oferta.
- Índices únicos parciales para idempotencia (RN-NO-04): `(turno_id, tipo, canal)` y `(oferta_destinatario_id, tipo, canal)`.

### integracion_whatsapp (DD-12)
- `id`, `phone_number_id`, `waba_id`, `access_token_cifrado BYTEA`, `app_secret_cifrado BYTEA`, `verify_token`, `estado ENUM('PENDIENTE','VERIFICADA','ERROR')`, `verificada_at`, `ultimo_error`
- Una sola fila por instancia. La carga el odontólogo desde el panel.

### integracion_google (post-MVP, DD-13)
- `id`, `usuario_id`, `refresh_token_cifrado BYTEA`, `calendar_id`, `conectado_at`

### evento_auditoria (append-only)
- `id BIGSERIAL`, `ocurrido_at`, `actor_tipo ENUM('USUARIO','PACIENTE','SISTEMA')`, `actor_id`, `accion VARCHAR(60)` (`LOGIN`, `VER_HISTORIA`, `CREAR_TURNO`, `EXPORTAR`…), `entidad`, `entidad_id`, `detalle JSONB`, `ip INET`
- Sin UPDATE ni DELETE. Retención mínima de 10 años (ver `12_cumplimiento_legal.md`).

## Seed data inicial

- 1 `consultorio` con los datos que carga el asistente de instalación.
- 1 `usuario` ODONTOLOGO (el email y la contraseña inicial se definen en la instalación; se fuerza el cambio en el primer login).
- `horario_atencion` de ejemplo: lunes a viernes de 9:00 a 13:00 y de 15:00 a 19:00.
- `prestacion` de ejemplo (editables): Consulta/diagnóstico 30 min · Limpieza 45 min · Obturación 45 min · Endodoncia 90 min · Extracción 45 min · Control 15 min.
- Catálogo fijo de estados del odontograma (enum en código, no en base).
