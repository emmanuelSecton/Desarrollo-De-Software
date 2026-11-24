# Decisiones y Supuestos

## Decisiones documentadas

### DD-01 — Una instancia de aplicación por consultorio (single-tenant) sobre infraestructura compartida
**Decisión**: el código sigue siendo single-tenant: cada consultorio tiene su propio contenedor de la aplicación y **su propia base de datos**, y el modelo no incluye `tenant_id`. Pero las instancias **comparten la infraestructura**:
- Un mismo host o plataforma de contenedores corre los N contenedores, detrás de un reverse proxy con TLS y un subdominio por consultorio.
- Un **único clúster PostgreSQL gestionado** aloja una base por consultorio, cada una con su propio usuario de base sin permisos sobre las demás.
- Los backups del clúster cubren todas las bases, y además se hace un `pg_dump` diario por base para poder restaurar un solo consultorio.

**Contexto**: la versión inicial (una instancia completa por cliente, con servidor y base gestionada propios) chocaba con la prioridad declarada de costo de operación (IN-01). El 2026-11-24 el usuario eligió esta variante.
**Alternativas consideradas**: (a) SaaS multi-tenant con `consultorio_id` y Row-Level Security; (b) instancia completa por cliente; (c) instancia por cliente sobre infraestructura compartida (elegida).
**Justificación**: baja mucho el costo marginal por cliente (un contenedor y una base dentro de un clúster existente, en lugar de un servidor y una base gestionada nuevos) **sin tocar el código ni el modelo**. El aislamiento de datos de salud se mantiene por base y credenciales separadas.
**Trade-offs aceptados**:
- **Radio de impacto compartido**: si se cae el clúster o el host, se caen todos los consultorios. Se mitiga con un clúster gestionado con alta disponibilidad y alertas.
- **Vecino ruidoso**: un consultorio con mucha carga afecta a los demás. Se mitiga con límites de CPU y memoria por contenedor y un pool de conexiones acotado por instancia.
- Hay que actualizar y migrar N instancias en cada versión, lo que exige automatización desde el día 1 (C-35).
- Migrar después a multi-tenant sigue implicando reescribir consultas y permisos.
**Revisión sugerida**: a los ~30 consultorios, si el clúster se satura, o si el costo de operar N contenedores supera al de una sola instalación multi-tenant.

### DD-02 — Monolito modular en Spring Boot + SPA React/Vite/TypeScript
**Decisión**: un solo backend con módulos por dominio y una SPA servida desde el mismo origen.
**Alternativas consideradas**: microservicios; Spring con vistas server-side (Thymeleaf).
**Justificación**: plazo acotado, un despliegue por instancia y bajo costo. React tiene el ecosistema más grande de componentes de calendario y formularios.
**Trade-offs aceptados**: escala como una unidad; se requiere disciplina para respetar los límites entre módulos.

### DD-03 — PostgreSQL gestionado
**Decisión**: PostgreSQL en un servicio de nube gestionado, con backups automáticos.
**Justificación**: la exclusion constraint (`btree_gist`) resuelve el anti-solapamiento en la base; JSONB sirve para el contenido clínico; el servicio gestionado reduce la operación.
**Trade-offs aceptados**: costo mensual del clúster, compartido entre todos los consultorios (DD-01).

### DD-04 — WhatsApp Cloud API oficial (no soluciones no oficiales)
**Decisión**: usar la WhatsApp Business Platform de Meta (Cloud API) con plantillas aprobadas.
**Alternativas consideradas**: librerías no oficiales sobre WhatsApp Web; solo click-to-chat manual.
**Justificación**: las soluciones no oficiales violan los términos de Meta y suelen terminar en bloqueos; el click-to-chat no automatiza. El estudio de mercado muestra que los competidores serios usan la API oficial.
**Trade-offs aceptados**: costo por mensaje, aprobación de plantillas, verificación del negocio y un número por consultorio.

### DD-05 — El paciente no tiene cuenta; se identifica por DNI y gestiona su turno por enlace firmado
**Decisión**: sin registro ni contraseña para el paciente.
**Justificación**: menos fricción para reservar; menos superficie de datos personales.
**Trade-offs aceptados**: cualquiera que conozca un DNI puede reservar a nombre de otro (mitigado por el límite RN-AG-06 y la notificación al teléfono registrado). **Suposición:** es aceptable para v1; ver PA-05.

### DD-06 — Reglas anti-ausentismo conservadoras en v1
**Decisión**: recordatorio a 48 h, aviso al consultorio a 24 h sin liberar automáticamente, ausencias solo registradas. La lista de espera la gestiona una persona, con oferta por WhatsApp con un clic (DD-10).
**Justificación**: elección del usuario en Discovery; menos riesgo de cancelar turnos por error y menos lógica automática para el MVP.
**Trade-offs aceptados**: la liberación automática de turnos no confirmados y la oferta en cascada sin intervención humana quedan para v2. El diferencial de la v1 se apoya en DD-10.

### DD-10 — Ofrecer un hueco liberado a la lista de espera con un clic
**Decisión**: cuando se libera un turno, la secretaria elige hasta 5 pacientes de la lista de espera y el sistema les envía a todos a la vez una plantilla de WhatsApp (`oferta_hueco`) con el botón "Lo tomo". **El primero que acepta se queda con el turno**; a los demás se les avisa que ya fue tomado. La oferta vence a los 30 minutos (configurable) o 2 h antes del hueco, lo que ocurra primero.
**Contexto**: con las reglas de DD-06 el diferencial contra ausencias quedaba débil frente a Bilog o Dentalink (IN-02). El 2026-11-24 el usuario eligió esta opción.
**Alternativas consideradas**: (a) lista de espera automática en cascada; (b) seña con Mercado Pago; (c) aceptar el diferencial débil y competir por precio; (d) oferta con un clic (elegida).
**Justificación**: el estudio de mercado mostró que **ningún producto argentino evidencia lista de espera**. Esta versión cierra el circuito (hueco → oferta → turno reasignado) sin que la secretaria llame uno por uno, y mantiene el control humano sobre a quién se le ofrece. Reutiliza piezas que la v1 ya tiene: WhatsApp oficial con webhooks y la exclusion constraint, que resuelve que "gane el primero" sin lógica extra de concurrencia.
**Trade-offs aceptados**:
- Más mensajes de plantilla: hasta 5 por hueco ofrecido.
- Meta podría clasificar la plantilla como "marketing" en lugar de "utilidad", lo que la encarece (R-06).
- Más lógica: una entidad nueva (`oferta_hueco`), un vencimiento programado y respuestas a quienes llegaron tarde.
- La cascada automática sigue quedando para v2.

### DD-07 — Historia clínica append-only con hash encadenado
**Decisión**: entradas inmutables, correcciones por enmienda y hash SHA-256 encadenado por paciente.
**Justificación**: Ley 26.529 (integridad, inalterabilidad, autoría) con una implementación simple y verificable.
**Trade-offs aceptados**: más volumen de datos; la interfaz debe mostrar las enmiendas con claridad.

### DD-08 — Sesión de servidor en vez de JWT
**Decisión**: cookie de sesión `HttpOnly` con CSRF.
**Justificación**: la SPA y la API están en el mismo origen; la revocación es inmediata y hay menos riesgo de robo de token desde JavaScript.

### DD-09 — Google Calendar unidireccional
**Decisión**: el sistema escribe en Google; no lee cambios desde Google en v1.
**Justificación**: la sincronización bidireccional agrega conflictos y complejidad sin ser crítica para el MVP.

### DD-11 — Hosting de la demo: un VPS económico con Docker Compose
**Decisión**: para la demo, un único VPS de bajo costo (por ejemplo Hetzner o DigitalOcean, del orden de USD 5-10/mes) con Docker Compose: reverse proxy con TLS automático (Caddy), los contenedores de la aplicación y **PostgreSQL en un contenedor** con volumen persistente y `pg_dump` diario a almacenamiento externo.
**Contexto**: PA-03. El usuario pidió simplicidad y bajo costo para una demo.
**Alternativas consideradas**: AWS São Paulo (EC2 + RDS); PaaS (Railway, Render); VPS + PostgreSQL gestionado.
**Justificación**: es lo más barato y simple de operar para una o pocas instancias de demo, y respeta el diseño de DD-01 (contenedor y base por consultorio, host compartido) con el mismo `docker-compose`.
**Trade-offs aceptados**:
- Sin alta disponibilidad ni backups gestionados.
- Región fuera de Argentina.
- **Solo se permiten datos ficticios** (SU-07).
- Antes del primer consultorio real se pasa a un PostgreSQL gestionado (PA-13).

### DD-12 — Cada odontólogo da de alta su propia cuenta de WhatsApp Business
**Decisión**: cada consultorio crea su Meta Business, su WhatsApp Business Account y su número, siguiendo una guía paso a paso del producto, y carga en el panel de configuración el `phone_number_id`, el token de sistema y el App Secret. El sistema valida las credenciales y registra el webhook.
**Contexto**: PA-04.
**Alternativas consideradas**: el proveedor gestiona las WABA como Tech Provider; Embedded Signup.
**Justificación**: el consultorio es dueño de su número y de la relación con Meta, paga sus mensajes directamente y el proveedor no necesita aprobarse como Tech Provider para el MVP.
**Trade-offs aceptados**:
- Más fricción en el alta del cliente: verificación del negocio y aprobación de plantillas por cada consultorio.
- Las plantillas deben someterse a aprobación en cada WABA.
- Las credenciales viven en la base cifradas (no en variables de entorno), así que hay que cifrarlas igual que el refresh token de Google.
- **Para la demo** se usa el número de prueba gratuito que Meta da en la Cloud API, con destinatarios de prueba registrados.

### DD-13 — Recorte de alcance para 1-2 personas en ~3 meses
**Decisión**: se postergan para después del MVP los changes de prioridad S:
- C-30, resumen de caja (US-052).
- C-31, sincronización con Google Calendar (US-014).
- C-34, indicadores de ausentismo (US-062).

El roadmap se replanifica para 2 personas.
**Contexto**: PA-01.
**Justificación**: son los únicos changes fuera del camino crítico y ninguno bloquea a otro.
**Trade-offs aceptados**:
- **Google Calendar**, elegida como integración en Discovery, queda fuera del MVP.
- Las métricas de éxito (`01_vision_y_objetivos.md`) se calculan con consultas manuales a la base hasta que exista C-34.
- Aun recortado, el plan tiene **34 changes en ~13 semanas**: muy ajustado para 2 personas e inviable para 1 sin más recortes (ver R-07).

## Supuestos inferidos

### SU-01 — Un único profesional por instancia
**Supuesto**: el MVP agenda a un solo odontólogo; no hay agenda por sillón.
**Origen**: segmento "odontólogo independiente" (Discovery).
**Riesgo si es falso**: consultorios de 2-3 profesionales que comparten sillón quedan afuera; hay que agregar `profesional_id` al turno y a la constraint.
**Cómo validar**: preguntar en las demos o entrevistas con 5 odontólogos.

### SU-02 — TypeScript es para el frontend ✅ Confirmado (PA-02, 2026-11-24)
**Supuesto**: el frontend es React + Vite + TypeScript y el backend Java.
**Origen**: el usuario pidió "Java y Spring Boot, y además TypeScript".
**Riesgo si es falso**: si se esperaba TypeScript en el backend (por ejemplo Node), cambia la arquitectura.
**Cómo validar**: confirmar con el usuario (PA-02).

### SU-03 — Los pacientes responden a WhatsApp
**Supuesto**: la mayoría de los pacientes tiene WhatsApp y responde a los botones.
**Riesgo si es falso**: el anti-ausentismo pierde efecto.
**Cómo validar**: piloto con un consultorio midiendo la tasa de confirmación (meta > 60 %).

### SU-04 — El MVP no necesita facturación ARCA
**Supuesto**: el independiente puede seguir facturando por fuera del sistema al principio.
**Riesgo si es falso**: barrera de adopción frente a Órbita, Oclusar o Dentia.
**Cómo validar**: preguntar en las demos; existe experiencia previa del equipo con ARCA en otro TP del repositorio.

### SU-05 — Web responsive alcanza
**Supuesto**: no hace falta app nativa; el odontólogo usa el panel desde la PC y el celular.
**Cómo validar**: pruebas de usabilidad en el celular.

### SU-07 — La demo usa solo datos ficticios
**Supuesto**: la entrega a ~3 meses es una demo con pacientes y odontólogos ficticios, no un consultorio real en producción.
**Origen**: respuesta a PA-03 ("simplicidad y bajo costo para una demo").
**Riesgo si es falso**: cargar datos de salud reales en el VPS de demo incumpliría las medidas de seguridad de la Ley 25.326 (sin backups gestionados ni alta disponibilidad, región fuera del país, sin DPA).
**Cómo validar**: confirmar con el usuario. Antes del primer cliente real, ejecutar la migración a producción (PA-13) y completar los entregables legales de C-36.

### SU-06 — Versiones Java 21 / Spring Boot 3 / PostgreSQL 16
**Supuesto**: son las LTS vigentes y no hay restricción de la cátedra.

## Riesgos

| ID | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| R-01 | Infraestructura compartida: si cae el clúster o el host caen todos los consultorios, un vecino ruidoso afecta a los demás, y hay que mantener N instancias | Medio (antes Alto; mitigado por DD-01 revisado) | Clúster gestionado con alta disponibilidad; límites de recursos por contenedor; despliegue y migraciones automatizados (C-35); `pg_dump` por base; revisar DD-01 a los ~30 consultorios |
| R-02 | Diferencial anti-ausentismo débil con las reglas conservadoras | Bajo (antes Medio; mitigado por DD-10) | Oferta de huecos con un clic (DD-10); medir los huecos recuperados (US-062); cascada automática en v2 |
| R-07 | Plazo: 34 changes en ~13 semanas con 1-2 personas | Alto | Plan de 2 personas (19 pasos, ~1 paso cada 4-5 días hábiles); si es 1 persona, recortar también C-32 (consulta de auditoría en UI) y reducir C-36 a lo indispensable para la demo; revisar el avance cada 2 semanas |
| R-06 | Meta clasifica la plantilla `oferta_hueco` como "marketing" (más cara o con restricciones) | Medio | Redactarla como respuesta a una solicitud del paciente (el paciente pidió estar en la lista de espera) y someterla a aprobación temprano (C-15); si la clasifican como marketing, limitar a 3 destinatarios por oferta |
| R-03 | Dependencia de Meta: aprobación de plantillas, verificación del negocio, costo por mensaje y un número por consultorio | Medio | Plantillas de "utilidad"; respaldo por email; proceso de alta documentado (ver `11_integracion_whatsapp.md`) |
| R-04 | Cumplimiento legal de la historia clínica electrónica y de los datos sensibles | Alto | Ver `12_cumplimiento_legal.md`; revisión por un asesor legal antes del lanzamiento |
| R-05 | Mercado con muchos competidores jóvenes y precios bajos | Medio | Precio publicado, prueba sin tarjeta, onboarding simple |
