# Spec Delta

## Purpose

Define el contrato observable del registro de auditoría de la instancia: huellas inalterables de acciones y lecturas sensibles, con roles de base con mínimo privilegio, tiempo en UTC y datos personales o clínicos excluidos del registro.

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
El registro de auditoría SHALL ser append-only: cualquier `UPDATE` o `DELETE` sobre `evento_auditoria` MUST ser rechazado por la base de datos, incluso por el dueño de la tabla.

#### Scenario: Intento de modificación
- **WHEN** una sesión ejecuta `UPDATE evento_auditoria` sobre un evento existente
- **THEN** PostgreSQL rechaza la sentencia con error y la fila permanece idéntica

#### Scenario: Intento de borrado
- **WHEN** una sesión ejecuta `DELETE FROM evento_auditoria`
- **THEN** PostgreSQL rechaza la sentencia con error y la fila permanece presente

### Requirement: Separación de roles de base
La base SHALL distinguir un rol migrator, dueño de la base que ejecuta Flyway y conserva DDL, de un rol runtime usado por la aplicación en operación normal. El rol runtime MUST tener sólo `SELECT` e `INSERT` sobre `evento_auditoria` y el `USAGE` necesario sobre su secuencia, sin `UPDATE` ni `DELETE`, y MUST NOT ejecutar migraciones.

#### Scenario: Permisos positivos del runtime
- **WHEN** el rol runtime consulta e inserta eventos de auditoría
- **THEN** ambas operaciones se ejecutan correctamente, incluida la asignación de id por la secuencia

#### Scenario: Permisos negados del runtime
- **WHEN** el rol runtime intenta `UPDATE` o `DELETE` sobre `evento_auditoria`
- **THEN** PostgreSQL deniega cada operación por falta de privilegio

#### Scenario: Migrator conserva DDL
- **WHEN** Flyway se ejecuta con el rol migrator al arrancar la instancia
- **THEN** las migraciones se aplican y el rol runtime sigue sin privilegios de escritura destructiva

### Requirement: Registro uniforme de escrituras y lecturas
El módulo de auditoría SHALL exponer una operación de registro que acepte acción, tipo de actor, identificador de actor opcional, entidad, identificador de entidad, detalle e IP opcional. Todo evento SHALL registrarse con el mismo contrato, tanto si la acción ocurre dentro de una transacción como si es una lectura sin transacción activa.

#### Scenario: Registro de escritura transaccional
- **WHEN** un módulo notifica una acción que crea, modifica o anula datos dentro de una transacción
- **THEN** el evento queda registrado con la acción, la entidad y el actor notificados

#### Scenario: Registro de lectura sin transacción
- **WHEN** se registra una lectura sensible como `VER_HISTORIA` sin transacción de negocio activa
- **THEN** el evento queda registrado con los mismos campos y sin requerir una transacción previa

### Requirement: Persistencia posterior a la confirmación
El sistema SHALL persistir el evento de auditoría después de que la transacción de negocio se confirma, en una transacción independiente. Un fallo en el registro SHALL NOT revertir ni alterar la operación de negocio ya confirmada.

#### Scenario: Confirmación y registro exitosos
- **WHEN** una transacción de negocio confirma y el registro posterior finaliza sin errores
- **THEN** la operación de negocio y el evento de auditoría quedan persistidos

#### Scenario: Fallo del registro posterior
- **WHEN** la transacción de negocio confirma pero el registro posterior falla
- **THEN** la operación de negocio permanece confirmada y el fallo queda registrado en el log con metadatos técnicos

### Requirement: Logging seguro de fallos de auditoría
Ante un fallo del registro posterior, el sistema SHALL emitir un log estructurado con acción, entidad, identificador de entidad y motivo técnico del fallo, y MUST NOT incluir datos clínicos, identificadores de paciente ni demás información personal.

#### Scenario: Metadatos presentes
- **WHEN** falla el registro de un evento con acción, entidad e identificador de entidad conocidos
- **THEN** el log contiene esos metadatos y el motivo técnico en campos estructurados

#### Scenario: Sin datos sensibles
- **WHEN** se inspecciona el log emitido por un fallo de auditoría
- **THEN** no aparece ningún dato clínico, nombre de paciente, email ni token

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
El campo `ip` SHALL ser nullable y, cuando se proporcione, MUST contener una dirección IP válida y no aceptar otro formato. La resolución de la IP real detrás de proxies SHALL diferirse a un change posterior.

#### Scenario: Evento sin IP
- **WHEN** se registra un evento del sistema sin dirección IP
- **THEN** la fila se acepta con `ip` nula

#### Scenario: IP válida
- **WHEN** se registra un evento con IP `200.24.18.7`
- **THEN** la fila se acepta con esa dirección en `ip`

#### Scenario: IP inválida
- **WHEN** se intenta registrar un evento con IP con formato inválido
- **THEN** el registro se rechaza antes de persistir

### Requirement: Detalle mínimo y whitelist de claves
El campo `detalle` SHALL contener únicamente metadatos mínimos de referencia, como claves e identificadores. El sistema MUST rechazar claves que correspondan a datos clínicos o sensibles, y MUST NOT incluir tales datos en logs ni en mensajes de error.

#### Scenario: Detalle permitido
- **WHEN** se registra un evento con detalle `{"turno_id": 42}`
- **THEN** el JSON se persiste con esa única clave de referencia

#### Scenario: Clave sensible
- **WHEN** se intenta registrar un evento con una clave de detalle fuera de la whitelist, como diagnóstico o email
- **THEN** el registro se rechaza sin persistir ni loguear el valor sensible

### Requirement: Índices para consulta futura
La tabla SHALL contener índices sobre `(ocurrido_at DESC)`, `(actor_tipo, actor_id)`, `(accion)` y `(entidad, entidad_id)` para sostener los filtros paginados de la consulta de auditoría.

#### Scenario: Índices presentes
- **WHEN** se inspeccionan los índices de `evento_auditoria` después de migrar
- **THEN** existen los cuatro índices declarados con esas columnas y orden
