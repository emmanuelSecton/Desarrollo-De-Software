# Spec Delta — plataforma-base

## MODIFIED Requirements

### Requirement: Prefijo de la API REST
Todos los endpoints REST de negocio SHALL publicarse bajo el prefijo `/api/v1`. Una ruta inexistente bajo `/api/v1` MUST responder en formato de error RFC 7807 y nunca con el `index.html` de la SPA: `404` cuando la pide un usuario con sesión, y `401` cuando se pide sin sesión, para no revelar qué rutas existen.

#### Scenario: Ruta de API inexistente
- **GIVEN** un usuario con sesión
- **WHEN** se hace `GET /api/v1/no-existe`
- **THEN** la respuesta es `404` con `Content-Type: application/problem+json`

#### Scenario: Ruta de API inexistente sin sesión
- **WHEN** se hace `GET /api/v1/no-existe` sin sesión
- **THEN** la respuesta es `401` con `Content-Type: application/problem+json`
