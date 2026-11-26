# Spec Delta — configuracion-consultorio

## MODIFIED Requirements

### Requirement: Consulta y modificación de los datos del consultorio
`GET /api/v1/consultorio` SHALL devolver nombre, profesional, matrícula, dirección, teléfono, email, `slugReserva`, los parámetros y la `zonaHoraria` del consultorio (identificador IANA configurado en la instancia, de solo lectura). `PUT /api/v1/consultorio` SHALL modificarlos con validación por campo y responder `200`; MUST ignorar la `zonaHoraria` recibida. El `slugReserva` MUST tener entre 3 y 60 caracteres en minúscula, dígitos o guiones.

#### Scenario: Odontólogo completa los datos
- **WHEN** el odontólogo envía nombre, profesional, matrícula y un slug válido
- **THEN** la respuesta es `200` con los datos guardados

#### Scenario: Slug inválido
- **WHEN** se envía `slugReserva` con mayúsculas, espacios o de 2 caracteres
- **THEN** la respuesta es `400` con el error en `slugReserva`

#### Scenario: Zona horaria del consultorio
- **GIVEN** una instancia configurada con `APP_TIMEZONE=America/Argentina/Buenos_Aires`
- **WHEN** el personal consulta `GET /api/v1/consultorio`
- **THEN** la respuesta incluye `zonaHoraria` `America/Argentina/Buenos_Aires`

#### Scenario: La zona horaria no se modifica por la API
- **WHEN** el odontólogo envía un `PUT` con otra `zonaHoraria`
- **THEN** la respuesta es `200` y la `zonaHoraria` sigue siendo la configurada
