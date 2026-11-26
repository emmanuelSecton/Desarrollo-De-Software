# Spec Delta

## Purpose

Define el contrato observable de la operación de cada consultorio sobre infraestructura compartida en la demo: alta aislada, imagen y migraciones versionadas, rollout con canarios y rollback, backups restaurables, límites de recursos, logs y alertas mínimas, sin exponer secretos ni datos reales.

## ADDED Requirements

### Requirement: Alta de un consultorio desde cero
El sistema SHALL permitir dar de alta un consultorio nuevo sobre la infraestructura compartida sin intervención manual en la base ni en el proxy, dejando operativos su subdominio HTTPS, su contenedor y su base. El alta MUST ser reproducible desde cero y SHALL dejar preparada la configuración de la instancia para su odontólogo sin crear aún entidades de negocio.

#### Scenario: Alta completa
- **WHEN** se da de alta un consultorio con un slug nuevo y su configuración
- **THEN** quedan creados su base, sus dos roles, su subdominio HTTPS, su contenedor y su configuración aislada, y la aplicación responde `UP` en `/actuator/health`

#### Scenario: Configuración preparada sin adelantar negocio
- **WHEN** se inspecciona la configuración de una instancia recién dada de alta
- **THEN** la variable `ADMIN_INITIAL_EMAIL` está lista para ser consumida por C-03 y no se crean usuario, consultorio ni credenciales de WhatsApp

### Requirement: Nombres y privilegios por instancia
Cada instancia SHALL usar una base `consultorio_<slug>`, un rol migrador `consultorio_<slug>_migrator` como dueño de esa base y un rol runtime `consultorio_<slug>_runtime` con `CONNECT` únicamente a su base y privilegios mínimos. La base MUST revocar `CONNECT` a `PUBLIC` y el runtime MUST NOT tener DDL ni privilegios destructivos sobre `evento_auditoria`.

#### Scenario: Convención de nombres
- **WHEN** se inspeccionan dos consultorios dados de alta con slugs distintos
- **THEN** cada uno tiene su base `consultorio_<slug>` y sus roles `consultorio_<slug>_migrator` y `consultorio_<slug>_runtime`, sin nombres compartidos entre instancias

#### Scenario: Cierre de acceso público
- **WHEN** se inspeccionan los permisos de una base de consultorio
- **THEN** `PUBLIC` no tiene `CONNECT` y el rol runtime indicado posee `CONNECT` y los privilegios mínimos necesarios para operar

#### Scenario: Runtime sin escritura destructiva
- **WHEN** el rol runtime intenta `UPDATE`, `DELETE` o `TRUNCATE` sobre `evento_auditoria`
- **THEN** la base rechaza cada sentencia y conserva las filas existentes

### Requirement: Aislamiento entre instancias
El rol runtime de una instancia MUST NOT poder conectarse a la base de otra instancia, y cada contenedor SHALL usar solo las credenciales de su propia base. El incumplimiento SHALL detectarse con una prueba de aislamiento sobre PostgreSQL real, nunca con H2 ni mocks de base.

#### Scenario: Conexión cruzada denegada
- **WHEN** el rol runtime del consultorio A intenta conectarse a la base del consultorio B
- **THEN** PostgreSQL rechaza la conexión por falta de privilegio

#### Scenario: Conexión propia permitida
- **WHEN** el mismo rol runtime se conecta a su propia base y ejecuta una consulta de salud
- **THEN** la conexión y la consulta finalizan correctamente sobre PostgreSQL 16 real

### Requirement: Imagen versionada y rollout con canarios
Todas las instancias SHALL desplegarse desde una única imagen versionada e inmutable, identificable por tag y digest. Una actualización SHALL aplicarse primero a dos consultorios de demo como canarios y ONLY THEN al resto; un fallo en la canaria MUST detener el rollout.

#### Scenario: Misma imagen para todas
- **WHEN** se despliegan dos consultorios de una misma versión
- **THEN** ambos contenedores informan el mismo tag/digest de imagen y ninguno usa una imagen construida de forma ad hoc

#### Scenario: Fallo en la canaria
- **WHEN** una de las dos instancias canarias falla su verificación de salud tras actualizar
- **THEN** el rollout se detiene antes de tocar las demás instancias y se informa el fallo

### Requirement: Migración por base antes del arranque
Cada base SHALL migrarse de forma independiente mediante un servicio efímero que ejecuta Flyway con el rol migrador y el placeholder `${runtime_role}` resuelto al rol runtime de esa instancia. La aplicación MUST arrancar con Flyway deshabilitado y ONLY AFTER que la migración de su base haya terminado correctamente. Las migraciones ya aplicadas MUST NOT reescribirse.

#### Scenario: Migrar todas las instancias
- **WHEN** se despliega una nueva versión de migraciones sobre todas las instancias
- **THEN** cada base aplica el mismo conjunto de versiones con su propio servicio efímero y su propio rol migrador, y todas las aplicaciones arrancan tras su migración

#### Scenario: Fallo de migración en una base
- **WHEN** falla la migración de una base
- **THEN** el contenedor de esa instancia no arranca y el rollout no continúa como si la migración hubiera tenido éxito

#### Scenario: Sin reescritura de historial
- **WHEN** se inspeccionan las migraciones aplicadas de una base ya migrada
- **THEN** no se han editado ni reejecutado las migraciones existentes y el placeholder `${runtime_role}` conserva el rol runtime de esa instancia

### Requirement: Rollback sin reescritura de auditoría
El procedimiento de rollback SHALL consistir en volver a la imagen anterior identificada por tag o digest y restaurar el backup `pg_dump` correspondiente. El sistema MUST NOT usar `flyway undo` ni `UPDATE`, `DELETE` o `TRUNCATE` sobre `evento_auditoria`: las correcciones SHALL registrarse como eventos o entradas nuevas.

#### Scenario: Rollback de una versión
- **WHEN** una versión desplegada debe revertirse
- **THEN** se vuelve al tag/digest anterior y se restaura el `pg_dump` de ese consultorio, sin ejecutar `flyway undo`

#### Scenario: Auditoría intacta tras rollback
- **WHEN** se inspecciona `evento_auditoria` después de un rollback
- **THEN** todas las filas previas siguen presentes y sin modificar, y cualquier corrección consta como un evento nuevo

### Requirement: Backup diario y restauración por consultorio
El sistema SHALL generar un `pg_dump` diario por base hacia un directorio externo montado o destino compatible configurable por variable, con retención de al menos 30 días. SHALL existir un procedimiento probado que restaure un solo consultorio desde su dump y verifique roles y grants resultantes.

#### Scenario: Backup diario con retención
- **WHEN** se ejecuta el job diario de backup durante más de 30 días
- **THEN** existe un `pg_dump` por base y se conservan los backups de al menos los últimos 30 días

#### Scenario: Restauración de un solo consultorio
- **WHEN** se restaura el `pg_dump` de un consultorio sin afectar a los demás
- **THEN** solo esa base queda reemplazada y sus roles y grants quedan verificados según la convención por instancia

#### Scenario: Fallo del job de backup
- **WHEN** el job de backup o el de migración falla
- **THEN** se emite una alerta técnica y el fallo no se reporta como backup correcto

### Requirement: Límites de recursos por instancia
Cada contenedor de la instancia SHALL tener límites de CPU y memoria, incluyendo `app`, PostgreSQL, el servicio de migración y el proxy. La conexión de cada aplicación SHALL usar un pool acotado a no más de cinco conexiones para evitar el agotamiento compartido (vecino ruidoso).

#### Scenario: Límites presentes
- **WHEN** se inspecciona la configuración de recursos de una instancia
- **THEN** `app`, PostgreSQL, migración y proxy tienen límites de CPU y/o memoria definidos y el pool de la aplicación está limitado a cinco conexiones o menos

#### Scenario: Vecino ruidoso
- **WHEN** una instancia consume más recursos de los permitidos
- **THEN** el límite de su contenedor evita que desplace a las demás instancias del mismo host

### Requirement: Secretos y configuración por instancia
Los secretos SHALL inyectarse mediante un `env_file` distinto por instancia y variables de entorno, nunca como valores en el repositorio. `.env.example` SHALL documentar las variables sin valores reales, incluidas `DB_POOL_MAX_SIZE`, `ADMIN_INITIAL_EMAIL` y `WA_VERIFY_TOKEN`.

#### Scenario: Configuraciones separadas
- **WHEN** se dan de alta dos consultorios
- **THEN** cada uno tiene su propio `env_file` con credenciales distintas y ningún valor secreto aparece versionado

#### Scenario: Ejemplo sin secretos
- **WHEN** se inspecciona `.env.example` y el runbook de operación
- **THEN** las variables sensibles figuran vacías y no contienen tokens, claves ni contraseñas reales

### Requirement: Logs estructurados y seguros
Los logs de operación SHALL emitirse en formato JSON y SOLO con los campos permitidos por la whitelist de MDC heredada de C-02, más metadatos técnicos de correlación. MUST NOT incluir datos clínicos, identificadores de paciente ni otra información personal.

#### Scenario: Log JSON válido
- **WHEN** la aplicación emite un log durante una operación de la instancia
- **THEN** la línea es JSON y contiene únicamente campos de la whitelist de C-02 y metadatos técnicos seguros

#### Scenario: Sin datos sensibles
- **WHEN** se inspeccionan los logs de una instancia en operación
- **THEN** no aparecen datos clínicos, nombres ni identificadores de pacientes, emails ni tokens

### Requirement: Alertas técnicas mínimas de la demo
La operación SHALL alertar cuando la aplicación deje de responder `/actuator/health`, cuando la base no esté disponible y cuando falle un job de backup o de migración. La alerta de token de Meta vencido SHALL diferirse a C-15/C-16.

#### Scenario: Salud de la aplicación
- **WHEN** `/actuator/health` de una instancia deja de responder o responde `DOWN`
- **THEN** se emite una alerta que identifica al consultorio afectado

#### Scenario: Base o job caído
- **WHEN** la base deja de responder o falla un job de backup o migración
- **THEN** se emite una alerta técnica sin datos clínicos ni secretos

#### Scenario: Token de Meta diferido
- **WHEN** se inspecciona el conjunto de alertas de este change
- **THEN** no hay alerta de token de Meta vencido, quedando prevista para C-15/C-16

### Requirement: Demo con TLS y solo datos ficticios
Cada consultorio de la demo SHALL ser alcanzable por su propio subdominio sobre HTTPS mediante un proxy con TLS automático, y la demo MUST utilizarse solo con datos ficticios. PostgreSQL SHALL ejecutarse en contenedor con volumen persistente; la migración a PostgreSQL gestionado y alta disponibilidad queda fuera de este change.

#### Scenario: Subdominio TLS por consultorio
- **WHEN** un consultorio de demo es dado de alta con su subdominio
- **THEN** su URL pública responde por HTTPS con certificado válido y enruta a su propio contenedor

#### Scenario: Solo datos ficticios
- **WHEN** se inspeccionan los datos cargados en la demo
- **THEN** no hay datos de pacientes reales ni información clínica verdadera
