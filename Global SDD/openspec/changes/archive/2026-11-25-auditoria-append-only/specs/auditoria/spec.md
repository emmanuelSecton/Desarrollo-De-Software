# Spec Delta

## Purpose

Define el contrato observable del registro de auditoría de la instancia: huellas inalterables de acciones y lecturas sensibles persistidas en la misma transacción de negocio, con roles de base con mínimo privilegio, tiempo en UTC y datos personales o clínicos excluidos del registro y de los logs.

## ADDED Requirements

### Requirement: Registro de auditoría con esquema definido
El sistema SHALL conservar cada evento en una tabla `evento_auditoria` con identificador secuencial, instante `TIMESTAMPTZ`, tipo e identificador de actor, acción, entidad, identificador de entidad, detalle JSONB y dirección IP opcional. La acción SHALL persistirse como texto de hasta 60 caracteres, no como tipo enumerado de base.

#### Scenario: Columnas y tipos
- **WHEN** se inspecciona el esquema tras aplicar la migración de auditoría sobre PostgreSQL 16 vacío
- **THEN** `evento_auditoria` existe con `id BIGSERIAL`, `ocurrido_at TIMESTAMPTZ`, `actor_tipo`, `actor_id`, `accion VARCHAR(60)`, `entidad`, `entidad_id`, `detalle JSONB` e `ip INET`

#### Scenario: Catálogo extensible sin migración
- **WHEN** se agrega un nuevo valor de acción al catálogo en código
- **THEN** no se requiere ninguna migración de base ni alteración de tipos para persistirlo

### Requirement: Inalterabilidad de los eventos
El registro de auditoría SHALL ser append-only: cualquier `UPDATE`, `DELETE` o `TRUNCATE` sobre `evento_auditoria` MUST ser rechazado por la base de datos, incluso por el dueño de la tabla. La protección de `TRUNCATE` SHALL implementarse con un trigger `BEFORE TRUNCATE ... FOR EACH STATEMENT`.

#### Scenario: Intento de modificación
- **WHEN** una sesión ejecuta `UPDATE evento_auditoria` sobre un evento existente
- **THEN** PostgreSQL rechaza la sentencia con error y la fila permanece idéntica

#### Scenario: Intento de borrado
- **WHEN** una sesión ejecuta `DELETE FROM evento_auditoria`
- **THEN** PostgreSQL rechaza la sentencia con error y la fila permanece presente

#### Scenario: Intento de truncado
- **WHEN** una sesión, incluso el dueño de la tabla, ejecuta `TRUNCATE TABLE evento_auditoria`
- **THEN** el trigger `BEFORE TRUNCATE ... FOR EACH STATEMENT` rechaza la sentencia con error y la tabla conserva todas sus filas

### Requirement: Separación de roles de base
La base SHALL distinguir un rol migrator (Flyway y DDL) de un rol runtime de la aplicación. El runtime MUST tener sólo `SELECT`/`INSERT` sobre `evento_auditoria` y `USAGE` sobre su secuencia, sin DDL ni migraciones. `app` SHALL usar sólo `DB_*` y no ejecutar Flyway; el servicio `migrate` de corta duración SHALL ejecutar Flyway como migrator. `APP_DB_USER` (por defecto `app_runtime`) SHALL resolverse en `${runtime_role}` en `V2__auditoria.sql`.

#### Scenario: Permisos positivos del runtime
- **WHEN** el rol runtime consulta e inserta eventos de auditoría
- **THEN** ambas operaciones se ejecutan correctamente, incluida la asignación de id por la secuencia

#### Scenario: Permisos negados del runtime
- **WHEN** el rol runtime intenta `UPDATE` o `DELETE` sobre `evento_auditoria`
- **THEN** PostgreSQL deniega cada operación por falta de privilegio

#### Scenario: Migrator conserva DDL
- **WHEN** el servicio `migrate` ejecuta Flyway con el rol migrator durante el despliegue y termina
- **THEN** las migraciones se aplican, el placeholder `${runtime_role}` se resuelve al valor de `APP_DB_USER` (por defecto `app_runtime`) en los grants de `V2__auditoria.sql`, y el rol runtime sigue sin privilegios de escritura destructiva ni de DDL

#### Scenario: Servicio app sin credenciales de migración
- **WHEN** se inspeccionan las credenciales del servicio `app` en el despliegue
- **THEN** `app` recibe sólo las variables `DB_*` del rol runtime y no ejecuta Flyway, que queda relegado al servicio `migrate` con credenciales de migrator

### Requirement: Registro uniforme de escrituras y lecturas
El módulo de auditoría SHALL exponer una operación de registro que acepte acción, tipo de actor, identificador de actor opcional, entidad, identificador de entidad, detalle e IP opcional. Todo evento SHALL registrarse con el mismo contrato, tanto si la acción ocurre dentro de una transacción como si es una lectura sin transacción activa.

#### Scenario: Registro de escritura transaccional
- **WHEN** un módulo notifica una acción que crea, modifica o anula datos dentro de una transacción
- **THEN** el evento queda registrado con la acción, la entidad y el actor notificados en esa misma transacción

#### Scenario: Registro de lectura sin transacción
- **WHEN** se registra una lectura sensible como `VER_HISTORIA` sin transacción de negocio activa
- **THEN** el evento queda registrado con los mismos campos en una transacción nueva, sin requerir una transacción previa

### Requirement: Persistencia transaccional del evento
El sistema SHALL registrar el evento en la MISMA transacción de negocio: `@TransactionalEventListener(phase = BEFORE_COMMIT, fallbackExecution = true)` y propagación `REQUIRED` en `AuditoriaServiceImpl`. Un fallo del registro MUST revertir la transacción de negocio (RN-GL-01 / Ley 26.529). Sin transacción activa, `fallbackExecution` SHALL abrir una transacción `REQUIRED` nueva para el evento.

#### Scenario: Confirmación conjunta de operación y evento
- **WHEN** una transacción de negocio notifica una acción y el registro de auditoría finaliza sin errores antes del commit
- **THEN** la operación de negocio y el evento de auditoría quedan confirmados juntos en la misma transacción

#### Scenario: Fallo del registro de auditoría
- **WHEN** el registro de auditoría falla antes de confirmar la transacción de negocio
- **THEN** la transacción se revierte, ni la operación ni el evento quedan persistidos, y el fallo se emite en el log con metadatos técnicos seguros

#### Scenario: Lectura sin transacción activa
- **WHEN** se notifica `VER_HISTORIA` sin ninguna transacción activa
- **THEN** `fallbackExecution = true` ejecuta el listener y `AuditoriaServiceImpl` abre una transacción `REQUIRED` nueva que persiste el evento

### Requirement: Logging seguro de fallos de auditoría
Ante un fallo del registro de auditoría, el sistema SHALL emitir un log estructurado únicamente con los campos prefijados `auditoria.accion`, `auditoria.entidad` y `auditoria.motivo`, más un identificador de correlación aleatorio. El log MUST NOT incluir `entidad_id`, identificadores de paciente, datos clínicos ni demás información personal.

#### Scenario: Metadatos presentes
- **WHEN** falla el registro de un evento con acción y entidad conocidas
- **THEN** el log contiene `auditoria.accion`, `auditoria.entidad`, `auditoria.motivo` y un identificador de correlación aleatorio, y no contiene `auditoria.entidad_id`

#### Scenario: Sin datos sensibles
- **WHEN** se inspecciona el log emitido por un fallo de auditoría
- **THEN** no aparece `entidad_id`, ningún identificador de paciente, dato clínico, nombre de paciente, email, IP ni token

### Requirement: Actores y restricción condicional
El sistema SHALL aceptar los tipos de actor `SISTEMA`, `PACIENTE` y `USUARIO`. La base MUST exigir `actor_id IS NULL` para `SISTEMA` y `actor_id IS NOT NULL` para `USUARIO` y `PACIENTE`, sin REFERENCES a tablas de usuario o paciente en este change.

#### Scenario: Actor sistema
- **WHEN** se registra un evento del sistema sin identificador de actor
- **THEN** la fila se acepta con `actor_tipo = 'SISTEMA'` y `actor_id` nulo

#### Scenario: Actor usuario
- **WHEN** se registra un evento de tipo `USUARIO` con identificador de actor
- **THEN** la fila se acepta con `actor_tipo = 'USUARIO'` y el identificador provisto

#### Scenario: Actor paciente
- **WHEN** se registra un evento de tipo `PACIENTE` con identificador de actor
- **THEN** la fila se acepta con `actor_tipo = 'PACIENTE'` y el identificador provisto

#### Scenario: Combinaciones inválidas
- **WHEN** se intenta registrar `SISTEMA` con identificador, o `USUARIO`/`PACIENTE` sin identificador
- **THEN** la base rechaza la fila por incumplir la restricción condicional

### Requirement: Tiempo del evento en UTC desde reloj inyectado
Cada evento SHALL llevar `ocurrido_at` igual al instante provisto por el reloj inyectado de la plataforma en UTC. El sistema MUST NOT obtener la hora con reloj del sistema directo ni aplicar un valor por defecto de base para esta columna.

#### Scenario: Reloj fijo
- **WHEN** el reloj inyectado se fija en `2026-11-24T13:00:00Z` y se registra un evento
- **THEN** `ocurrido_at` es exactamente `2026-11-24T13:00:00Z`

#### Scenario: Sin reloj directo
- **WHEN** se inspecciona el código que produce `ocurrido_at` y la definición de columna
- **THEN** no hay llamadas a reloj del sistema sin inyección ni `DEFAULT now()` en la columna

### Requirement: Dirección IP opcional y validada
El campo `ip` SHALL ser nullable y, cuando se proporcione, MUST contener una dirección IP válida y no aceptar otro formato. El parseo SHALL ser puramente sintáctico y MUST NOT resolver nombres de host ni realizar consultas DNS. La resolución de la IP real detrás de proxies SHALL diferirse a un change posterior.

#### Scenario: Evento sin IP
- **WHEN** se registra un evento del sistema sin dirección IP
- **THEN** la fila se acepta con `ip` nula

#### Scenario: IP válida
- **WHEN** se registra un evento con IP `200.24.18.7`
- **THEN** la fila se acepta con esa dirección en `ip`

#### Scenario: IP inválida
- **WHEN** se intenta registrar un evento con IP con formato inválido
- **THEN** el registro se rechaza antes de persistir

#### Scenario: Sin resolución DNS
- **WHEN** se valida una dirección IP o se inspecciona el validador
- **THEN** la validación es pura, comprueba sólo la sintaxis y no intenta resolver hosts ni consultar DNS

### Requirement: Detalle mínimo y whitelist de claves
El campo `detalle` SHALL contener únicamente metadatos mínimos de referencia, como claves e identificadores. El valor de la clave `referencia` MUST coincidir con `^[A-Za-z0-9_-]{1,64}$`. El sistema MUST rechazar claves que correspondan a datos clínicos o sensibles, y MUST NOT incluir tales datos en logs ni en mensajes de error.

#### Scenario: Detalle permitido
- **WHEN** se registra un evento con detalle `{"turno_id": 42}`
- **THEN** el JSON se persiste con esa única clave de referencia

#### Scenario: Referencia válida
- **WHEN** se registra un evento con detalle `{"referencia": "turno-42_A1"}`
- **THEN** el JSON se persiste con esa referencia

#### Scenario: Referencia inválida
- **WHEN** se intenta registrar un evento con `referencia` vacía, de más de 64 caracteres o con caracteres fuera de `^[A-Za-z0-9_-]{1,64}$`
- **THEN** el registro se rechaza antes de persistir y el valor rechazado no se incluye en logs ni en mensajes de error

#### Scenario: Clave sensible
- **WHEN** se intenta registrar un evento con una clave de detalle fuera de la whitelist, como diagnóstico o email
- **THEN** el registro se rechaza sin persistir ni loguear el valor sensible

### Requirement: Validación de la solicitud de registro
Las validaciones en Java SHALL rechazar `accion`, `tipoActor`, `entidad` y `entidadId` cuando sean nulos o superen su longitud permitida (60, 20, 80 y 100 caracteres respectivamente), lanzando `AuditoriaInvalidaException` antes de publicar o persistir el evento. El mensaje de la excepción y los logs MUST NOT incluir el valor rechazado.

#### Scenario: Campo nulo
- **WHEN** se construye una solicitud de registro con `accion`, `tipoActor`, `entidad` o `entidadId` nulo
- **THEN** se lanza `AuditoriaInvalidaException` y no se persiste ni publica el evento

#### Scenario: Campo con longitud excedida
- **WHEN** se construye una solicitud con `accion`, `tipoActor`, `entidad` o `entidadId` por encima de su longitud permitida
- **THEN** se lanza `AuditoriaInvalidaException` y no se persiste ni publica el evento

#### Scenario: Valor rechazado no replicado
- **WHEN** se inspecciona el mensaje de `AuditoriaInvalidaException` y el log asociado
- **THEN** el valor rechazado no aparece en ninguno de los dos

### Requirement: Índices para consulta futura
La tabla SHALL contener índices sobre `(ocurrido_at DESC)`, `(actor_tipo, actor_id)`, `(accion)` y `(entidad, entidad_id)` para sostener los filtros paginados de la consulta de auditoría.

#### Scenario: Índices presentes
- **WHEN** se inspeccionan los índices de `evento_auditoria` después de migrar
- **THEN** existen los cuatro índices declarados con esas columnas y orden

### Requirement: Pruebas del repositorio con rol runtime
Las pruebas de `RepositorioAuditoriaJdbc` SHALL ejecutarse contra PostgreSQL 16 real con un `DataSource` autenticado como el rol runtime no superusuario creado por el script de inicialización de Testcontainers, y MUST NOT usar H2 ni mocks de la base.

#### Scenario: Inserción y lectura con rol runtime
- **WHEN** `RepositorioAuditoriaJdbc` inserta y lee un evento usando el `DataSource` del rol runtime no superusuario
- **THEN** ambas operaciones finalizan correctamente sobre el contenedor PostgreSQL real

#### Scenario: Escritura destructiva denegada
- **WHEN** la misma conexión de runtime intenta `UPDATE`, `DELETE` o `TRUNCATE` sobre `evento_auditoria`
- **THEN** la base rechaza cada sentencia por privilegios insuficientes o por el trigger append-only
