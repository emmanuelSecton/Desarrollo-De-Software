# pacientes Specification

## Purpose

Define cómo el consultorio registra, encuentra y mantiene los datos personales y de contacto de sus pacientes, con DNI único, consentimiento registrado (Ley 25.326), conservación sin borrado físico (Ley 26.529) y trazabilidad de cada cambio.

## Requirements

### Requirement: Paciente identificado por DNI único
El sistema SHALL guardar cada paciente con un DNI único de 7 a 9 dígitos, almacenado sin puntos ni espacios. La base MUST rechazar un segundo paciente con el mismo DNI.

#### Scenario: DNI con puntos
- **WHEN** se da de alta un paciente con DNI `30.123.456`
- **THEN** queda guardado como `30123456`

#### Scenario: DNI repetido
- **GIVEN** existe un paciente con DNI `30123456`
- **WHEN** se intenta crear otro con el mismo DNI
- **THEN** la base rechaza la inserción

### Requirement: Alta interna de pacientes
`POST /api/v1/pacientes` SHALL crear un paciente con DNI, nombre, apellido, celular y consentimiento de datos obligatorios, y fecha de nacimiento, email y consentimiento de WhatsApp opcionales. Responde `201` con el paciente creado. Si el DNI ya existe MUST responder `409` en RFC 7807 con el id del paciente existente, sin modificarlo.

#### Scenario: Alta correcta
- **WHEN** el personal envía los datos obligatorios válidos
- **THEN** la respuesta es `201` con el paciente, `activo = true` y `ausenciasCount = 0`

#### Scenario: DNI existente
- **GIVEN** existe un paciente con DNI `30123456`
- **WHEN** se intenta dar de alta otro con ese DNI
- **THEN** la respuesta es `409` con la propiedad `pacienteId` del existente y sus datos quedan igual

#### Scenario: Datos inválidos
- **WHEN** falta el apellido, el DNI tiene letras, el email no es válido o la fecha de nacimiento es futura
- **THEN** la respuesta es `400` con el error de cada campo

### Requirement: Consentimiento registrado con fecha
Toda alta SHALL exigir el consentimiento para el tratamiento de datos y guardar su fecha y la versión de la política de privacidad aceptada (RN-DP-01). El consentimiento para recibir WhatsApp SHALL ser opcional y, si se da, guardarse con su fecha. Retirar el consentimiento de WhatsApp MUST dejar de habilitar ese canal.

#### Scenario: Alta sin consentimiento de datos
- **WHEN** se intenta un alta sin aceptar el tratamiento de datos
- **THEN** la respuesta es `400` y no se guarda nada

#### Scenario: Consentimiento de WhatsApp
- **WHEN** se da de alta un paciente que acepta WhatsApp
- **THEN** queda `aceptaWhatsapp = true` con la fecha del consentimiento tomada del reloj del sistema

#### Scenario: Revocación de WhatsApp
- **GIVEN** un paciente que aceptó WhatsApp
- **WHEN** se lo modifica con `aceptaWhatsapp = false`
- **THEN** queda `aceptaWhatsapp = false` y sin fecha de consentimiento de WhatsApp

### Requirement: Teléfono normalizado a E.164
El sistema SHALL normalizar el celular a formato E.164, interpretando números sin prefijo internacional como argentinos. Un número que no sea un celular válido MUST rechazarse con `400`.

#### Scenario: Formatos argentinos habituales
- **WHEN** se carga `011 15-2345-6789`, `+54 9 11 2345-6789` o `1123456789`
- **THEN** se guarda `+5491123456789`

#### Scenario: Número inválido
- **WHEN** se carga `12345`
- **THEN** la respuesta es `400` con el error en el campo del teléfono

### Requirement: Búsqueda de pacientes
`GET /api/v1/pacientes?q=` SHALL buscar pacientes activos por DNI, por nombre o apellido sin distinguir mayúsculas ni acentos, o por teléfono, y devolver una página ordenada por apellido y nombre. Con `incluirInactivos=true` MUST incluir también los dados de baja.

#### Scenario: Búsqueda por apellido sin acentos
- **GIVEN** existe la paciente "Ana Pérez"
- **WHEN** se busca `perez`
- **THEN** el resultado la incluye

#### Scenario: Búsqueda por DNI o teléfono
- **WHEN** se busca por el DNI completo o por el celular en cualquier formato aceptado
- **THEN** el resultado incluye a ese paciente

#### Scenario: Pacientes dados de baja
- **GIVEN** un paciente dado de baja
- **WHEN** se busca sin `incluirInactivos`
- **THEN** no aparece; con `incluirInactivos=true` sí aparece

### Requirement: Consulta y modificación
`GET /api/v1/pacientes/{id}` SHALL devolver los datos del paciente y `PUT /api/v1/pacientes/{id}` SHALL modificar sus datos personales y de contacto con las mismas validaciones que el alta. Un id inexistente MUST responder `404`. El DNI MUST poder corregirse solo si no queda repetido; el contador de ausencias y la fecha del consentimiento de datos MUST NOT modificarse por esta vía.

#### Scenario: Modificación de contacto
- **WHEN** se actualiza el celular y el email de un paciente
- **THEN** la respuesta es `200` con los datos nuevos y el teléfono normalizado

#### Scenario: Corrección de DNI que choca con otro paciente
- **WHEN** se cambia el DNI de un paciente por el de otro existente
- **THEN** la respuesta es `409` y no cambia nada

#### Scenario: Paciente inexistente
- **WHEN** se pide un id que no existe
- **THEN** la respuesta es `404` en formato RFC 7807

### Requirement: Baja lógica sin borrado físico
`DELETE /api/v1/pacientes/{id}` SHALL marcar al paciente como inactivo sin borrarlo (RN-HC-03), y `POST /api/v1/pacientes/{id}/reactivar` SHALL volver a activarlo. El rol runtime de la base MUST NOT poder borrar filas de `paciente`.

#### Scenario: Baja
- **WHEN** el odontólogo da de baja a un paciente
- **THEN** la respuesta es `204`, el paciente sigue existiendo con `activo = false` y se lo puede consultar por id

#### Scenario: El rol runtime no puede borrar
- **WHEN** la aplicación, con el rol runtime, ejecuta un `DELETE` sobre `paciente`
- **THEN** la base lo rechaza por falta de permisos

### Requirement: Permisos por rol
El odontólogo y la secretaria SHALL poder buscar, dar de alta, consultar y modificar pacientes. Solo el odontólogo SHALL poder dar de baja o reactivar (03_actores_y_roles.md). Sin sesión, toda ruta de pacientes MUST responder `401`.

#### Scenario: Secretaria intenta dar de baja
- **WHEN** una secretaria envía `DELETE /api/v1/pacientes/{id}`
- **THEN** la respuesta es `403` y el paciente sigue activo

#### Scenario: Secretaria gestiona contacto
- **WHEN** una secretaria da de alta, busca y modifica un paciente
- **THEN** las tres operaciones se ejecutan

### Requirement: Obtener o crear por DNI para otros módulos
El módulo SHALL ofrecer a otros módulos una operación que, dado un DNI y los datos de alta, devuelva el paciente existente **sin modificar ninguno de sus datos** o, si no existe, lo cree con esos datos. Dos llamadas simultáneas con el mismo DNI MUST terminar con un único paciente.

#### Scenario: DNI existente con otros datos
- **GIVEN** existe un paciente con DNI `30123456` y celular `+5491123456789`
- **WHEN** otro módulo pide obtener o crear ese DNI con otro celular
- **THEN** recibe el paciente existente y su celular no cambia

#### Scenario: Altas simultáneas
- **WHEN** dos llamadas concurrentes piden obtener o crear el mismo DNI nuevo
- **THEN** existe un solo paciente con ese DNI y ambas llamadas reciben su id

### Requirement: Auditoría de cambios en pacientes
Cada alta, modificación, baja y reactivación SHALL registrar un evento de auditoría (`CREAR_ENTIDAD`, `MODIFICAR_ENTIDAD`, `ANULAR_ENTIDAD`) con entidad `PACIENTE`, el id del paciente y el usuario que actuó (RN-GL-01). Ningún evento MUST incluir DNI, nombre, teléfono ni email.

#### Scenario: Modificación auditada
- **WHEN** la secretaria modifica un paciente
- **THEN** existe un evento `MODIFICAR_ENTIDAD` con actor `USUARIO`, su id y el id del paciente, sin datos personales

#### Scenario: Alta fallida sin evento
- **WHEN** un alta se rechaza por DNI duplicado
- **THEN** no se registra ningún evento de alta
