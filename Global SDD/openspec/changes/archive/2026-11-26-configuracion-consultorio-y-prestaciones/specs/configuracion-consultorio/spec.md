# Spec Delta — configuracion-consultorio

## Purpose

Define la configuración de la que depende la agenda: los datos del consultorio y sus parámetros de recordatorio y cancelación, las franjas de atención por día y el catálogo de prestaciones con su duración, con las reglas que garantizan que sean coherentes.

## ADDED Requirements

### Requirement: Un solo consultorio por instancia
Cada instancia SHALL tener exactamente un registro de consultorio, creado por las migraciones con valores provisorios. La base MUST impedir una segunda fila y el rol runtime MUST NOT poder insertar ni borrar consultorios.

#### Scenario: Instancia nueva
- **WHEN** se aplican las migraciones a una base vacía
- **THEN** existe un consultorio con recordatorio a 48 h, límite de confirmación a 24 h, cancelación mínima a 24 h y vencimiento de ofertas a 30 min

#### Scenario: Segundo consultorio
- **WHEN** se intenta insertar otra fila en `consultorio`
- **THEN** la base la rechaza

### Requirement: Consulta y modificación de los datos del consultorio
`GET /api/v1/consultorio` SHALL devolver nombre, profesional, matrícula, dirección, teléfono, email, `slugReserva` y los parámetros. `PUT /api/v1/consultorio` SHALL modificarlos con validación por campo y responder `200`. El `slugReserva` MUST tener entre 3 y 60 caracteres en minúscula, dígitos o guiones.

#### Scenario: Odontólogo completa los datos
- **WHEN** el odontólogo envía nombre, profesional, matrícula y un slug válido
- **THEN** la respuesta es `200` con los datos guardados

#### Scenario: Slug inválido
- **WHEN** se envía `slugReserva` con mayúsculas, espacios o de 2 caracteres
- **THEN** la respuesta es `400` con el error en `slugReserva`

### Requirement: Parámetros de recordatorio, confirmación y cancelación coherentes
El recordatorio SHALL estar entre 1 y 168 horas antes del turno; el límite de confirmación MUST ser al menos 1 hora y menor que el recordatorio; la cancelación mínima MUST estar entre 0 y 168 horas; el vencimiento de ofertas MUST estar entre 5 y 1440 minutos. La base MUST rechazar valores fuera de esos rangos aunque la validación de la API se saltee.

#### Scenario: Confirmación posterior al recordatorio
- **WHEN** se configura recordatorio a 24 h y límite de confirmación a 36 h
- **THEN** la respuesta es `400` y los parámetros no cambian

#### Scenario: Valores válidos
- **WHEN** se configura recordatorio a 72 h, confirmación a 12 h y cancelación a 6 h
- **THEN** la respuesta es `200` y los valores quedan guardados

### Requirement: Franjas de atención por día
El sistema SHALL guardar franjas de atención por día de la semana (1 = lunes a 7 = domingo) con hora de inicio menor que la de fin, en múltiplos de 5 minutos. Dos franjas del mismo día MUST NOT superponerse; la base MUST garantizarlo. Las franjas se crean, modifican y borran con `POST`, `PUT` y `DELETE /api/v1/horarios/{id}`, y se listan ordenadas por día y hora con `GET /api/v1/horarios`.

#### Scenario: Horarios iniciales
- **WHEN** se lista una instancia nueva
- **THEN** hay franjas de lunes a viernes de 09:00 a 13:00 y de 15:00 a 19:00

#### Scenario: Franja superpuesta
- **GIVEN** una franja del lunes de 09:00 a 13:00
- **WHEN** se crea otra del lunes de 12:00 a 14:00
- **THEN** la respuesta es `409` y no se guarda

#### Scenario: Franjas contiguas
- **GIVEN** una franja del lunes de 09:00 a 13:00
- **WHEN** se crea otra del lunes de 13:00 a 14:00
- **THEN** se guarda

#### Scenario: Franja inválida
- **WHEN** se envía una franja de 13:00 a 09:00, de 09:07 a 10:00 o con día 8
- **THEN** la respuesta es `400`

### Requirement: Catálogo de prestaciones
El sistema SHALL guardar prestaciones con nombre único entre las activas, duración en minutos mayor que 0 y múltiplo de 5 (RN-AG-02), precio de referencia opcional no negativo y la marca `reservableOnline`. Las prestaciones MUST NOT borrarse: `DELETE /api/v1/prestaciones/{id}` las da de baja y `POST /api/v1/prestaciones/{id}/reactivar` las vuelve a activar. `GET /api/v1/prestaciones` lista las activas, y con `incluirInactivas=true` también las dadas de baja.

#### Scenario: Prestaciones iniciales
- **WHEN** se lista una instancia nueva
- **THEN** están Consulta/diagnóstico 30, Limpieza 45, Obturación 45, Endodoncia 90, Extracción 45 y Control 15, todas activas

#### Scenario: Duración inválida
- **WHEN** se crea una prestación de 0 o de 32 minutos
- **THEN** la respuesta es `400` y la base también rechazaría el valor

#### Scenario: Baja lógica
- **WHEN** el odontólogo da de baja una prestación
- **THEN** la respuesta es `204`, deja de aparecer en el listado por defecto y sigue existiendo

#### Scenario: Nombre repetido
- **WHEN** se crea una prestación activa con el nombre de otra activa, sin distinguir mayúsculas
- **THEN** la respuesta es `409`

### Requirement: Permisos de configuración
El odontólogo y la secretaria SHALL poder leer la configuración, los horarios y las prestaciones. Solo el odontólogo SHALL poder modificarlos (03_actores_y_roles.md). Sin sesión, todas las rutas MUST responder `401`.

#### Scenario: Secretaria intenta modificar
- **WHEN** una secretaria envía `PUT /api/v1/consultorio`, `POST /api/v1/horarios` o `POST /api/v1/prestaciones`
- **THEN** la respuesta es `403` y nada cambia

#### Scenario: Secretaria consulta
- **WHEN** una secretaria pide la configuración, los horarios o las prestaciones
- **THEN** la respuesta es `200`

### Requirement: Auditoría de la configuración
Cada modificación del consultorio, y cada alta, cambio, baja o borrado de horarios y prestaciones SHALL registrar un evento de auditoría con el usuario que actuó, la entidad (`CONSULTORIO`, `HORARIO_ATENCION` o `PRESTACION`) y su id.

#### Scenario: Cambio de parámetros auditado
- **WHEN** el odontólogo cambia el horario del recordatorio
- **THEN** existe un evento `MODIFICAR_ENTIDAD` con entidad `CONSULTORIO`, el id del consultorio y el id del odontólogo como actor
