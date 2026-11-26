# frontend-agenda Specification

## Purpose

Define cómo el personal del consultorio ve la agenda y da, confirma, reprograma y cancela turnos desde la pantalla, en la zona horaria del consultorio y mostrando de forma clara los rechazos de la API.

## Requirements

### Requirement: Vista día y vista semana
La sección `/agenda` SHALL mostrar los turnos de un día (vista por defecto) o de una semana de lunes a domingo, pedidos a `GET /api/v1/turnos` con el rango correspondiente, con navegación a la fecha anterior, a hoy y a la siguiente. La fecha y la vista elegidas MUST quedar en la URL, de modo que recargar la página muestre lo mismo.

#### Scenario: Agenda de hoy
- **WHEN** el personal abre `/agenda`
- **THEN** ve los turnos de hoy ordenados por hora, cada uno con hora de inicio y fin, paciente y prestación

#### Scenario: Cambio a semana
- **WHEN** elige la vista semana
- **THEN** ve los siete días de esa semana con sus turnos, y la URL indica la vista y la fecha

#### Scenario: Día sin turnos
- **WHEN** el día elegido no tiene turnos
- **THEN** la agenda muestra un estado vacío con la acción de dar un turno

### Requirement: Horarios en la zona del consultorio
La agenda SHALL mostrar los días y las horas en la zona horaria del consultorio (`zonaHoraria` de `GET /api/v1/consultorio`) y MUST enviar a la API los inicios en esa zona, sin depender de la zona del navegador.

#### Scenario: Navegador en otra zona
- **GIVEN** el consultorio en `America/Argentina/Buenos_Aires` y el navegador en UTC
- **WHEN** se da un turno el lunes a las 10:00
- **THEN** la API recibe el inicio `10:00-03:00` y la agenda lo muestra a las 10:00

### Requirement: Estado visible con texto e ícono
Cada turno SHALL mostrar su estado con texto e ícono además del color: Reservado, Confirmado, Cancelado, Atendido o Ausente. Un turno `RESERVADO` cuyo inicio cae dentro de `confirmacionLimiteHoras` del consultorio MUST mostrarse como "Sin confirmar". Los sobreturnos MUST llevar la marca "Sobreturno". Los turnos cancelados MUST ocultarse por defecto y verse atenuados, sin ocupar el horario, al activar "Mostrar cancelados".

#### Scenario: Turno sin confirmar
- **GIVEN** un límite de confirmación de 24 horas
- **WHEN** un turno `RESERVADO` empieza dentro de 20 horas
- **THEN** se ve con la etiqueta "Sin confirmar"

#### Scenario: Sobreturno
- **WHEN** un turno es sobreturno
- **THEN** se ve con la marca "Sobreturno" junto a su estado

#### Scenario: Cancelados ocultos
- **WHEN** el día tiene un turno cancelado
- **THEN** no se ve hasta activar "Mostrar cancelados", y entonces aparece atenuado con la etiqueta "Cancelado"

### Requirement: Franjas de atención visibles
La vista SHALL marcar las franjas de atención del día (`GET /api/v1/horarios`) y atenuar el tiempo fuera de ellas. Dar un turno fuera de una franja MUST ser posible para el personal, con una advertencia visible.

#### Scenario: Día sin atención
- **WHEN** el día elegido no tiene franjas de atención
- **THEN** la vista indica "Sin horario de atención" y sigue permitiendo dar un turno

### Requirement: Dar un turno con búsqueda o alta de paciente
El diálogo "Dar turno" SHALL permitir buscar un paciente activo por DNI, nombre o teléfono (`GET /api/v1/pacientes?q=`) o darlo de alta en el momento (`POST /api/v1/pacientes`, con el consentimiento de datos obligatorio), elegir una prestación activa, el día, la hora en múltiplos de 5 minutos y la duración (por defecto la de la prestación, extensible de a 5 minutos). Al crear el turno (`POST /api/v1/turnos`) la agenda MUST mostrarlo sin recargar la página.

#### Scenario: Turno para un paciente existente
- **WHEN** la secretaria busca "Martínez", elige a la paciente, "Limpieza", el lunes a las 10:00 y confirma
- **THEN** el turno aparece en la agenda a las 10:00 como "Reservado" y se anuncia "Turno dado"

#### Scenario: Paciente nuevo en el momento
- **WHEN** la búsqueda no encuentra al paciente y la secretaria lo da de alta con DNI, nombre, apellido, teléfono y consentimiento
- **THEN** el paciente queda seleccionado para el turno

#### Scenario: Errores por campo
- **WHEN** la API responde `400` con errores por campo (por ejemplo, teléfono inválido o DNI duplicado)
- **THEN** cada mensaje se ve junto a su campo y no se pierde lo cargado

### Requirement: Superposición y sobreturno
Si la API rechaza un alta o una reprogramación con `409` de tipo `/problemas/turno-superpuesto`, la pantalla SHALL explicar que ese horario ya está ocupado y permitir elegir otro. Al dar un turno MUST ofrecer además darlo como sobreturno, que se envía solo tras una confirmación explícita del usuario (RN-AG-07). La reprogramación no ofrece sobreturno, porque la API no lo admite al mover un turno.

#### Scenario: Horario ocupado
- **GIVEN** un turno activo de 10:00 a 10:45
- **WHEN** se intenta dar otro a las 10:30
- **THEN** se ve "Ese horario se superpone con otro turno" con las opciones "Elegir otro horario" y "Dar como sobreturno"

#### Scenario: Reprogramar a un horario ocupado
- **WHEN** se reprograma un turno a un horario ocupado
- **THEN** se ve el mismo mensaje con la opción "Elegir otro horario", sin sobreturno, y el turno original sigue igual

#### Scenario: Sobreturno confirmado
- **WHEN** el usuario elige "Dar como sobreturno" y lo confirma
- **THEN** el turno se crea con la marca de sobreturno y aparece en la agenda

### Requirement: Acciones sobre un turno según su estado
Al elegir un turno la pantalla SHALL mostrar su detalle y solo las acciones que su estado permite (RN-TU-01): "Confirmar" para `RESERVADO`; "Reprogramar" y "Cancelar" para `RESERVADO` y `CONFIRMADO`. Los turnos cancelados, atendidos o ausentes MUST mostrarse sin acciones.

#### Scenario: Confirmar
- **WHEN** se confirma un turno `RESERVADO`
- **THEN** pasa a "Confirmado" en la agenda

#### Scenario: Cancelar con motivo
- **WHEN** se cancela indicando quién cancela y un motivo
- **THEN** el turno se ve "Cancelado" y libera el horario
- **AND** si el motivo está vacío el diálogo no se envía y marca el campo

#### Scenario: Reprogramar
- **WHEN** se reprograma un turno a otro día y hora libres
- **THEN** el turno aparece en el horario elegido y el anterior queda cancelado con motivo "Reprogramado" (visible con "Mostrar cancelados")

### Requirement: Ediciones simultáneas
Si la API responde `409` de tipo `/problemas/turno-modificado`, la pantalla SHALL avisar "Otra persona modificó este turno. Actualizamos la agenda para que veas los cambios.", recargar los turnos y no aplicar la acción.

#### Scenario: Turno confirmado por otra persona
- **GIVEN** la agenda abierta en dos computadoras
- **WHEN** una confirma un turno y la otra intenta cancelarlo con la versión anterior
- **THEN** la segunda ve el aviso y el turno "Confirmado"

### Requirement: Accesibilidad de la agenda
La agenda SHALL poder usarse con teclado: cada turno es un control con nombre accesible que incluye hora, paciente y estado; los diálogos llevan el foco al primer campo y lo devuelven al cerrar. Las altas, cambios y errores MUST anunciarse (`role="status"` o `role="alert"`). La pantalla MUST usar solo los tokens del sistema de diseño.

#### Scenario: Turno con teclado
- **WHEN** el usuario recorre la agenda con Tab y presiona Enter sobre un turno
- **THEN** se abre su detalle con el nombre "10:00, Lucía Martínez, Limpieza, Reservado"
