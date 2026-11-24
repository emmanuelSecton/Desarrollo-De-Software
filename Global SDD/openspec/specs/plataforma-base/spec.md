# plataforma-base Specification

## Purpose

Define el comportamiento técnico transversal de cada instancia del sistema (salud, formato de errores, prefijo de la API, tiempo y zona horaria, configuración, esquema base, entrega de la SPA y límites entre módulos) del que dependen todos los módulos de dominio.

## Requirements

### Requirement: Endpoint de salud de la instancia
El sistema SHALL exponer `GET /actuator/health` sin autenticación, respondiendo `200` con `{"status":"UP"}` cuando la aplicación y su base de datos están disponibles, y `503` con `{"status":"DOWN"}` cuando la base no responde. La respuesta MUST NOT incluir detalles internos (componentes, versiones, URLs ni credenciales).

#### Scenario: Instancia sana
- **WHEN** la aplicación arrancó y PostgreSQL responde
- **THEN** `GET /actuator/health` devuelve `200` con `status` igual a `UP`

#### Scenario: Base de datos caída
- **WHEN** la aplicación está corriendo y PostgreSQL deja de responder
- **THEN** `GET /actuator/health` devuelve `503` con `status` igual a `DOWN`

#### Scenario: Sin detalles internos
- **WHEN** se consulta `GET /actuator/health`
- **THEN** el cuerpo contiene solo `status` y no incluye `components`, `details`, URL JDBC ni nombres de usuario de base

### Requirement: Solo el endpoint de salud del actuator queda expuesto
El sistema SHALL exponer por HTTP únicamente el endpoint `health` del actuator; cualquier otro endpoint de actuator MUST responder `404`.

#### Scenario: Endpoint de actuator no expuesto
- **WHEN** se consulta `GET /actuator/env` o `GET /actuator/beans`
- **THEN** la respuesta es `404`

### Requirement: Prefijo de la API REST
Todos los endpoints REST de negocio SHALL publicarse bajo el prefijo `/api/v1`. Una ruta inexistente bajo `/api/v1` MUST responder `404` en formato de error RFC 7807 y nunca con el `index.html` de la SPA.

#### Scenario: Ruta de API inexistente
- **WHEN** se hace `GET /api/v1/no-existe`
- **THEN** la respuesta es `404` con `Content-Type: application/problem+json`

### Requirement: Formato uniforme de errores RFC 7807
Toda respuesta de error de la API SHALL usar `Content-Type: application/problem+json` con los campos `type`, `title`, `status`, `detail` e `instance` (RFC 7807 / RFC 9457).

#### Scenario: Error con formato de problema
- **WHEN** un endpoint bajo `/api/v1` responde con un código 4xx o 5xx
- **THEN** el cuerpo es un objeto JSON con `type`, `title`, `status` (igual al código HTTP), `detail` e `instance` (la ruta pedida)

### Requirement: Errores de validación detallados por campo
Cuando la entrada de un endpoint no cumple sus validaciones, el sistema SHALL responder `400` en formato RFC 7807 con una propiedad `errores` que lista, por cada campo inválido, el `campo` y un `mensaje` en español.

#### Scenario: Cuerpo inválido
- **WHEN** se envía a un endpoint de la API un cuerpo con un campo obligatorio vacío
- **THEN** la respuesta es `400` con `Content-Type: application/problem+json` y `errores` contiene un elemento con ese `campo` y su `mensaje`

#### Scenario: JSON mal formado
- **WHEN** se envía a un endpoint de la API un cuerpo que no es JSON válido
- **THEN** la respuesta es `400` en formato RFC 7807 sin exponer el mensaje del parser

### Requirement: Errores inesperados sin filtrar información interna
Ante una excepción no prevista, el sistema SHALL responder `500` en formato RFC 7807 con un `detail` genérico, sin stack trace, nombres de clases, SQL ni datos de la petición, y MUST registrar el error en el log sin datos clínicos ni personales.

#### Scenario: Excepción no controlada
- **WHEN** un endpoint de la API lanza una excepción no prevista
- **THEN** la respuesta es `500` con `Content-Type: application/problem+json`, un `detail` genérico y ningún stack trace ni nombre de clase en el cuerpo

### Requirement: Fechas almacenadas y transmitidas en UTC
El sistema SHALL almacenar todo instante en columnas `TIMESTAMPTZ` interpretadas en UTC (RN-GL-02) y la sesión de base de cada conexión de la aplicación MUST usar la zona `UTC`, independientemente de la zona del host. La API SHALL serializar instantes en ISO-8601 con desplazamiento explícito, nunca como números.

#### Scenario: Zona de la sesión de base
- **WHEN** la aplicación abre una conexión a PostgreSQL en un host cuya zona no es UTC
- **THEN** `current_setting('TimeZone')` en esa conexión devuelve `UTC`

#### Scenario: Serialización de instantes
- **WHEN** la API devuelve un instante
- **THEN** se serializa como texto ISO-8601 con desplazamiento (por ejemplo `2026-11-24T13:00:00Z`) y no como número de época

### Requirement: Zona horaria del consultorio configurable
El sistema SHALL tomar la zona horaria del consultorio de la configuración `APP_TIMEZONE`, con valor por defecto `America/Argentina/Buenos_Aires`, y usarla para convertir instantes UTC a fecha y hora locales y viceversa. Una zona inválida MUST impedir el arranque con un mensaje que nombre la variable.

#### Scenario: Zona por defecto
- **WHEN** la aplicación arranca sin `APP_TIMEZONE`
- **THEN** la zona del consultorio es `America/Argentina/Buenos_Aires` y el instante `2026-11-24T13:00:00Z` se presenta como `2026-11-24 10:00` local

#### Scenario: Zona inválida
- **WHEN** la aplicación arranca con `APP_TIMEZONE=Marte/Olympus`
- **THEN** el arranque falla con un mensaje que menciona `APP_TIMEZONE`

### Requirement: Reloj único e inyectable
Toda lógica que necesite la hora actual SHALL obtenerla de un reloj único provisto por la plataforma (en UTC), de modo que los tests puedan fijarlo en un instante determinado.

#### Scenario: Reloj fijado en tests
- **WHEN** un test reemplaza el reloj de la plataforma por uno fijo en `2026-11-24T13:00:00Z`
- **THEN** los componentes que piden la hora actual obtienen exactamente ese instante

### Requirement: Configuración por variables de entorno
La configuración de la instancia (conexión a la base, URL pública, zona horaria y futuras credenciales) SHALL provenir de variables de entorno. El repositorio MUST incluir un `.env.example` que documente cada variable sin valores reales y MUST NOT contener secretos.

#### Scenario: Variable obligatoria ausente
- **WHEN** la aplicación arranca sin `DB_URL` o sin `APP_BASE_URL`
- **THEN** el arranque falla con un mensaje que nombra la variable faltante

#### Scenario: Ejemplo sin secretos
- **WHEN** se inspecciona `.env.example`
- **THEN** cada variable de `knowledge-base/08_arquitectura_propuesta.md` §Variables de entorno figura con una descripción y ninguna variable sensible tiene un valor real

### Requirement: Esquema base gestionado solo por migraciones
El esquema de la base SHALL crearse y evolucionar únicamente mediante migraciones Flyway versionadas aplicadas al arrancar; la migración inicial MUST habilitar las extensiones `btree_gist`, `citext` y `pgcrypto`. La aplicación MUST NOT crear ni alterar tablas por otro medio y MUST negarse a arrancar si el esquema no coincide con lo que espera.

#### Scenario: Base vacía
- **WHEN** la aplicación arranca contra una base PostgreSQL 16 vacía
- **THEN** Flyway aplica `V1__baseline` y las extensiones `btree_gist`, `citext` y `pgcrypto` quedan instaladas

#### Scenario: Arranque repetido
- **WHEN** la aplicación vuelve a arrancar contra una base ya migrada
- **THEN** no se reaplica ninguna migración y el arranque termina correctamente

### Requirement: Entrega de la SPA desde el mismo origen
La aplicación SHALL servir el build de la SPA desde el mismo origen que la API. Las rutas `GET` que no son de la API, del actuator ni archivos estáticos existentes MUST devolver el `index.html` de la SPA, para que funcionen los enlaces directos a rutas del cliente.

#### Scenario: Raíz del sitio
- **WHEN** se hace `GET /`
- **THEN** la respuesta es `200` con el `index.html` de la SPA

#### Scenario: Enlace directo a una ruta del cliente
- **WHEN** se hace `GET /agenda/semana` (ruta que solo existe en la SPA)
- **THEN** la respuesta es `200` con el `index.html` de la SPA

### Requirement: Límites entre módulos verificados automáticamente
El build SHALL fallar si el código viola la arquitectura del monolito modular: dentro de cada módulo las dependencias van `api → application → domain`, con `infrastructure` implementando puertos; un módulo solo usa la API pública de otro módulo; `shared` no depende de ningún módulo de dominio.

#### Scenario: Violación de capas
- **WHEN** una clase de `domain` de un módulo depende de una clase de `api` o `infrastructure`
- **THEN** la verificación de arquitectura falla y el build no pasa

#### Scenario: Uso de clases internas de otro módulo
- **WHEN** una clase de `agenda` depende de una clase de `pacientes.infrastructure` o `pacientes.domain`
- **THEN** la verificación de arquitectura falla y el build no pasa

#### Scenario: Estructura conforme
- **WHEN** el código respeta las reglas de capas y de módulos
- **THEN** la verificación de arquitectura pasa
