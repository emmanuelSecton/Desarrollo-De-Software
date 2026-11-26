# frontend-shell Specification

## Purpose

Define cómo el personal del consultorio entra a la aplicación web, mantiene y pierde su sesión, navega según su rol y ve los errores de la API, de forma accesible y consistente con el sistema de diseño.

## Requirements

### Requirement: Ingreso con email y contraseña
La pantalla `/login` SHALL pedir email y contraseña, enviarlos a `POST /api/v1/auth/login` con el token CSRF y, si son válidos, llevar al usuario a la sección inicial de la app. Ante un rechazo MUST mostrar un único mensaje genérico, sin indicar si el email existe o si la cuenta está bloqueada.

#### Scenario: Ingreso correcto
- **WHEN** el usuario ingresa credenciales válidas
- **THEN** ve la agenda como sección inicial, con su nombre en la barra superior

#### Scenario: Credenciales rechazadas
- **WHEN** la API responde `401`
- **THEN** la pantalla muestra "El email o la contraseña no son válidos, o la cuenta está bloqueada temporalmente" y el foco vuelve al campo de email

#### Scenario: Campos vacíos
- **WHEN** se intenta ingresar con un campo vacío
- **THEN** el campo queda marcado como inválido con su mensaje, sin llamar a la API

### Requirement: Cambio de contraseña obligatorio
Si la sesión tiene el cambio de contraseña pendiente, la app SHALL mostrar solo la pantalla `/cambiar-contrasena` hasta que el cambio se complete, y MUST mostrar los errores de la API en el campo correspondiente.

#### Scenario: Primer ingreso
- **GIVEN** un usuario con `debeCambiarPassword = true`
- **WHEN** inicia sesión o intenta abrir otra sección
- **THEN** ve la pantalla de cambio de contraseña

#### Scenario: Cambio exitoso
- **WHEN** ingresa la contraseña actual y una nueva válida
- **THEN** ve la agenda y un aviso "Contraseña actualizada"

### Requirement: Navegación según el rol
La barra de navegación SHALL mostrar Agenda y Pacientes a todo el personal, y Configuración solo al odontólogo. Una ruta restringida pedida por otro rol MUST mostrar la página de acceso denegado.

#### Scenario: Secretaria
- **WHEN** una secretaria entra a la app
- **THEN** ve Agenda y Pacientes, y no ve Configuración

#### Scenario: Secretaria abre una ruta del odontólogo
- **WHEN** una secretaria abre `/configuracion` por URL
- **THEN** ve la página de acceso denegado

### Requirement: Sesión y su vencimiento
La app SHALL recuperar la sesión al cargar (`GET /api/v1/auth/me`) y, si no hay, llevar al login conservando la ruta pedida. Tras 28 minutos sin actividad MUST avisar que la sesión está por vencer y permitir seguir; si la API responde `401` en cualquier momento, MUST volver al login con el mensaje "Tu sesión venció. Volvé a ingresar."

#### Scenario: Recarga con sesión activa
- **WHEN** el usuario recarga la página con una sesión vigente
- **THEN** sigue en la misma sección sin volver a ingresar

#### Scenario: Aviso de inactividad
- **WHEN** pasan 28 minutos sin actividad
- **THEN** aparece un diálogo con "Seguir conectado" y "Salir"; elegir seguir renueva la sesión

#### Scenario: Sesión vencida
- **WHEN** una llamada a la API responde `401`
- **THEN** el usuario ve el login con el aviso de sesión vencida y, al ingresar, vuelve a la sección donde estaba

### Requirement: Cliente HTTP con CSRF y errores uniformes
Toda llamada a `/api/v1` SHALL enviar la cookie de sesión y, en métodos que modifican, la cabecera `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN`. Las respuestas de error en `application/problem+json` MUST convertirse en un error con título, detalle y errores por campo, para que cada pantalla los muestre sin interpretar la respuesta cruda.

#### Scenario: POST con CSRF
- **WHEN** la app envía un `POST`
- **THEN** la petición lleva `X-XSRF-TOKEN` igual a la cookie `XSRF-TOKEN`

#### Scenario: Error por campo
- **WHEN** la API responde `400` con `errores`
- **THEN** el formulario muestra cada mensaje junto a su campo

### Requirement: Páginas de error
Una ruta inexistente SHALL mostrar la página "No encontramos esa página" con un enlace a la agenda; un acceso denegado SHALL mostrar "No tenés acceso a esta sección" con el mismo enlace.

#### Scenario: Ruta inexistente
- **WHEN** el usuario abre `/no-existe`
- **THEN** ve la página de no encontrada con el enlace a la agenda

### Requirement: Accesibilidad y sistema de diseño
Las pantallas SHALL usar solo los tokens de `design-system/tokens.css` (sin colores, espaciados ni tipografías fijos), tener etiquetas asociadas a cada campo, foco visible, objetivos táctiles de 44 px y anunciar los errores con `role="alert"`. El estado nunca MUST comunicarse solo con color.

#### Scenario: Error anunciado
- **WHEN** el login falla
- **THEN** el mensaje está en un contenedor con `role="alert"` y el campo tiene `aria-invalid`
