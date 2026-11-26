# Spec Delta — agenda-turnos

## Purpose

Define cómo el personal del consultorio da, consulta, mueve, cancela y confirma turnos, con la garantía en la base de que dos turnos activos nunca se superponen, transiciones de estado válidas y protección ante ediciones simultáneas.

## ADDED Requirements

### Requirement: Alta de turno
`POST /api/v1/turnos` SHALL crear un turno `RESERVADO` de origen `INTERNO` para un paciente activo y una prestación activa, con inicio en múltiplos de 5 minutos y posterior al momento actual. La duración MUST ser la de la prestación o mayor, en múltiplos de 5 minutos y de hasta 480 (RN-AG-02). Responde `201` con el turno, su `version` y su token público.

#### Scenario: Turno con la duración de la prestación
- **GIVEN** la prestación "Limpieza" de 45 minutos
- **WHEN** se crea un turno a las 10:00 sin indicar duración
- **THEN** la respuesta es `201` con fin a las 10:45 y estado `RESERVADO`

#### Scenario: Duración extendida
- **WHEN** se crea un turno de "Limpieza" con duración de 60 minutos
- **THEN** el turno termina 60 minutos después del inicio

#### Scenario: Datos inválidos
- **WHEN** la duración es menor que la de la prestación, no es múltiplo de 5, el inicio es pasado o no es múltiplo de 5 minutos
- **THEN** la respuesta es `400` con el error del campo

#### Scenario: Paciente o prestación no disponibles
- **WHEN** el paciente no existe o está dado de baja, o la prestación no existe o está inactiva
- **THEN** la respuesta es `400` y no se crea nada

### Requirement: Sin superposición de turnos activos
La base SHALL impedir dos turnos en estado `RESERVADO` o `CONFIRMADO` cuyos intervalos se superpongan, salvo que alguno sea sobreturno (RN-AG-01). Un intento de crear o mover un turno que se superpone MUST responder `409` con un mensaje claro que indique que puede crearse como sobreturno. Los turnos contiguos MUST aceptarse.

#### Scenario: Turno superpuesto
- **GIVEN** un turno activo de 10:00 a 10:45
- **WHEN** se crea otro de 10:30 a 11:00
- **THEN** la respuesta es `409` con `type` `/problemas/turno-superpuesto`

#### Scenario: Turnos contiguos
- **GIVEN** un turno activo de 10:00 a 10:45
- **WHEN** se crea otro de 10:45 a 11:15
- **THEN** se crea

#### Scenario: Altas simultáneas
- **WHEN** dos pedidos simultáneos crean turnos superpuestos
- **THEN** uno se crea y el otro responde `409`

#### Scenario: Horario de un turno cancelado
- **GIVEN** un turno cancelado de 10:00 a 10:45
- **WHEN** se crea otro en el mismo horario
- **THEN** se crea

### Requirement: Sobreturno explícito
Con `sobreturno = true`, el sistema SHALL crear el turno aunque se superponga con otros, marcado como sobreturno y fuera de la garantía anti-solapamiento (RN-AG-07). Sin esa marca, nunca MUST crearse un turno superpuesto.

#### Scenario: Sobreturno sobre un turno ocupado
- **GIVEN** un turno activo de 10:00 a 10:45
- **WHEN** se crea otro a las 10:15 con `sobreturno = true`
- **THEN** la respuesta es `201` y el turno queda marcado como sobreturno

### Requirement: Estados y transiciones válidas
Los turnos SHALL seguir RN-TU-01: desde `RESERVADO` se puede pasar a `CONFIRMADO`, `CANCELADO`, `ATENDIDO` o `AUSENTE`; desde `CONFIRMADO`, a `CANCELADO`, `ATENDIDO` o `AUSENTE`. `CANCELADO`, `ATENDIDO` y `AUSENTE` son finales. Una transición inválida MUST responder `409` sin cambiar el turno.

#### Scenario: Confirmación por el personal
- **WHEN** el personal confirma un turno `RESERVADO` con `POST /api/v1/turnos/{id}/confirmar`
- **THEN** queda `CONFIRMADO`

#### Scenario: Transición desde un estado final
- **WHEN** se intenta confirmar o cancelar un turno `CANCELADO`
- **THEN** la respuesta es `409` con `type` `/problemas/transicion-invalida`

### Requirement: Cancelación con motivo
`POST /api/v1/turnos/{id}/cancelar` SHALL pasar a `CANCELADO` un turno `RESERVADO` o `CONFIRMADO`, exigiendo un motivo de hasta 200 caracteres e indicando si cancela el paciente o el consultorio. El horario queda libre para otro turno.

#### Scenario: Cancelación
- **WHEN** la secretaria cancela un turno con motivo "El paciente avisó por teléfono" y `canceladoPor = PACIENTE`
- **THEN** la respuesta es `200`, el turno queda `CANCELADO` con motivo y quién canceló

#### Scenario: Sin motivo
- **WHEN** se cancela sin motivo
- **THEN** la respuesta es `400`

### Requirement: Reprogramación sin dos turnos activos
`PUT /api/v1/turnos/{id}` con un nuevo inicio (y opcionalmente otra duración) SHALL cancelar el turno original con motivo "Reprogramado" y crear uno nuevo vinculado con `reprogramadoDe`, en la misma transacción (RN-TU-03). En ningún momento MUST haber dos turnos activos del mismo reprogramado. Si el nuevo horario se superpone con otro turno, MUST responder `409` y el original queda como estaba.

#### Scenario: Mover un turno 15 minutos
- **GIVEN** un turno de 10:00 a 10:45
- **WHEN** se lo reprograma a las 10:15
- **THEN** el original queda `CANCELADO`, existe uno nuevo de 10:15 a 11:00 vinculado al original y el nuevo no choca con el original

#### Scenario: Mover a un horario ocupado
- **WHEN** se reprograma a un horario que pisa otro turno activo
- **THEN** la respuesta es `409` y el turno original sigue activo y sin cambios

### Requirement: Protección ante ediciones simultáneas
Cancelar, confirmar o reprogramar SHALL exigir la `version` del turno que el cliente leyó. Si el turno cambió desde entonces, MUST responder `409` con `type` `/problemas/turno-modificado` sin aplicar el cambio.

#### Scenario: Versión vieja
- **GIVEN** un turno que otra persona ya confirmó
- **WHEN** se intenta cancelarlo con la versión anterior
- **THEN** la respuesta es `409` y el turno sigue `CONFIRMADO`

### Requirement: Consulta de turnos
`GET /api/v1/turnos?desde&hasta` SHALL devolver los turnos que empiezan en ese rango, de hasta 31 días, ordenados por inicio, con el nombre y apellido del paciente y el nombre de la prestación. `GET /api/v1/turnos/{id}` SHALL devolver un turno o `404`.

#### Scenario: Turnos del día
- **WHEN** se piden los turnos de un día
- **THEN** la respuesta incluye los de ese día, activos y cancelados, ordenados por inicio, cada uno con su estado

#### Scenario: Rango demasiado largo
- **WHEN** el rango supera los 31 días o `desde` es posterior a `hasta`
- **THEN** la respuesta es `400`

### Requirement: Token público de cada turno
Cada turno SHALL tener un token aleatorio de 256 bits, único y no secuencial, para el enlace público que usarán los mensajes al paciente (RN-GL-03).

#### Scenario: Tokens distintos
- **WHEN** se crean dos turnos
- **THEN** cada uno tiene un token distinto de 43 caracteres URL-safe

### Requirement: Permisos y auditoría de turnos
El odontólogo y la secretaria SHALL poder usar todos los endpoints de turnos; sin sesión MUST responderse `401`. Cada alta SHALL auditarse como `CREAR_TURNO`, cada confirmación o reprogramación como `MODIFICAR_ENTIDAD` y cada cancelación como `ANULAR_ENTIDAD`, con entidad `TURNO`, su id y el usuario, sin datos del paciente ni el motivo de cancelación.

#### Scenario: Secretaria da un turno
- **WHEN** una secretaria crea un turno
- **THEN** la respuesta es `201` y existe un evento `CREAR_TURNO` con su id como actor

#### Scenario: Sin sesión
- **WHEN** se pide `/api/v1/turnos` sin sesión
- **THEN** la respuesta es `401`

### Requirement: Eventos de dominio de turnos
El sistema SHALL publicar `TurnoCreado`, `TurnoCancelado`, `TurnoReprogramado` y `TurnoEstadoCambiado` con los ids del turno y del paciente, que otros módulos MUST recibir solo después de confirmada la transacción.

#### Scenario: Evento tras el commit
- **WHEN** se crea un turno y la transacción se confirma
- **THEN** un oyente de `AFTER_COMMIT` recibe `TurnoCreado` con el id del turno

#### Scenario: Sin evento si falla
- **WHEN** la creación falla por superposición
- **THEN** ningún oyente de `AFTER_COMMIT` recibe `TurnoCreado`
