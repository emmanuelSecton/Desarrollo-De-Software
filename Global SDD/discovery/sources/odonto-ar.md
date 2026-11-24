# Odonto.ar

- **URL**: https://odonto.ar/
- **Empresa proveedora**: No evidenciado (términos y privacidad solo nombran "Odonto.ar")
- **País de origen**: Argentina (privacidad indica sede en la ciudad de Córdoba; precios en ARS vía Mercado Pago)
- **Fecha de investigación**: 2026-11-23

<!-- Las 8 secciones siguientes son el esquema fijo de web-scraper: van siempre y en este orden. -->

## Problema que resuelve

Software de gestión para consultorios y clínicas odontológicas con agenda, turnos online, odontograma, historia clínica, presupuestos y finanzas.

## Usuarios objetivo

Odontólogo independiente y clínicas pequeñas (planes de 1, 5 y 10 usuarios).

## Features detectadas

- Agenda diaria, semanal y mensual compartida con el equipo
- Reserva online por enlace con disponibilidad, duración y reglas configurables
- Odontograma interactivo con historial de quién cambió qué y cuándo
- Historia clínica, radiografías y estudios, presupuestos y tratamientos, tarifario
- Obras sociales con coberturas configurables y cálculo de parte de la obra social y del paciente
- Panel de finanzas; importación desde Excel

## Pricing

- Prueba Gratuita: USD 0 por 30 días, 3 usuarios, todas las funciones, sin tarjeta.
- Individual: USD 29/mes (aprox. ARS 46.400), 1 usuario.
- Profesional: USD 49/mes (aprox. ARS 78.400), 5 usuarios (recomendado).
- Clínica: USD 99/mes (aprox. ARS 158.400), 10 usuarios.
- Facturado en USD, pagado en pesos por Mercado Pago al tipo de cambio del día; sin renovación automática. Promociones para estudiantes por consulta.

## Posicionamiento

Gestión odontológica accesible para consultorios argentinos, con turnos online y odontograma (afirmación comercial).

## Diferenciadores

(Afirmaciones comerciales) Precios publicados y planes por cantidad de usuarios; prueba de 30 días con todas las funciones; auditoría de cambios en el odontograma; autogestión de turnos del paciente.

## Notas del investigador

- Los precios provienen de la sección de precios del home (resumen vía WebFetch); la equivalencia en pesos es aproximada, según el sitio.
- La página /demo es una demo interactiva de 7 pasos; solo se pudo ver el primero (reserva del paciente). La demo muestra duración por motivo de consulta (30/30/60 min) y que el paciente confirma DNI y teléfono y solo ve/mueve sus propios turnos.
- La respuesta de la FAQ sobre seguridad no apareció en la extracción.
- Términos: la exportación se puede pedir dentro de 30 días tras terminar la cuenta; no se describe exportación en cuenta activa. Los respaldos no se mencionan en ninguno de los documentos legales.
- WhatsApp figura solo como canal de contacto comercial; no hay integración.
- Sin reseñas en marketplaces ni cifras de clientes; razón social no identificada. Sitio de una sola página con secciones ancla, sin centro de ayuda.
- No se identificó mención de periodontograma, sillones/boxes, lista de espera ni ARCA.

## Relevamiento detallado

Estado de cada fila:
- **Comprobado**: se ve en documentación pública, centro de ayuda, capturas o videos oficiales que muestran el producto funcionando, o en la página de precios.
- **Declarado**: solo aparece como afirmación de marketing en la landing.
- **No evidenciado**: no se encontró evidencia pública.

| Dimensión | Detalle | Estado | Fuente (URL) |
|---|---|---|---|
| Modalidad (SaaS / nube / app móvil / local / híbrida) | SaaS web (base de datos en Supabase) | Comprobado (privacidad) | https://odonto.ar/privacidad |
| Agenda: vista diaria/semanal | Vistas diaria, semanal y mensual | Declarado | https://odonto.ar/ |
| Agenda: múltiples profesionales | Agenda compartida con el equipo; planes de 5 y 10 usuarios | Comprobado (planes) | https://odonto.ar/ |
| Agenda: múltiples sillones/boxes | No mencionado (la demo muestra "Consultorio Sur" como sede) | No evidenciado | https://odonto.ar/demo |
| Agenda: duración variable por prestación | Duración por motivo de turno (30/30/60 min) en la demo; duración configurable declarada | Comprobado (demo) | https://odonto.ar/demo |
| Agenda: bloqueos, sobreturnos, prevención de solapamientos | "Reglas de reserva" configurables; sin detalle | Declarado | https://odonto.ar/ |
| Turnos: reserva online 24/7 / enlace / web / redes | Enlace compartible para que el paciente reserve | Comprobado (demo, paso 1) | https://odonto.ar/demo |
| Turnos: chatbot / WhatsApp | WhatsApp solo como contacto comercial | No evidenciado | https://odonto.ar/ |
| Turnos: confirmación, cancelación, reprogramación por el paciente | El paciente valida DNI y teléfono y puede ver y mover sus propios turnos | Comprobado (demo) | https://odonto.ar/demo |
| Turnos: lista de espera | No mencionado | No evidenciado | https://odonto.ar/ |
| Automatización: recordatorios y confirmaciones (canal) | No mencionado | No evidenciado | https://odonto.ar/ |
| Automatización: ausentes, recuperación, controles periódicos, campañas | No mencionado | No evidenciado | https://odonto.ar/ |
| Clínica: ficha clínica / anamnesis | Historia clínica | Declarado | https://odonto.ar/ |
| Clínica: odontograma | Interactivo, con historial de cambios; la política de privacidad menciona odontograma, tratamientos y notas clínicas como datos tratados | Declarado (funcionalidad); mención en privacidad | https://odonto.ar/privacidad |
| Clínica: periodontograma | No mencionado | No evidenciado | https://odonto.ar/ |
| Clínica: imágenes / radiografías | Radiografías y estudios | Declarado | https://odonto.ar/ |
| Clínica: presupuestos y planes de tratamiento | Presupuestos y tratamientos, tarifario | Declarado | https://odonto.ar/ |
| Admin: caja, cobros, señas | Panel de finanzas; caja y señas no detalladas | Declarado | https://odonto.ar/ |
| Admin: facturación electrónica (AFIP/ARCA u otra local) | No mencionada | No evidenciado | https://odonto.ar/ |
| Admin: Mercado Pago u otra pasarela | Mercado Pago solo para cobrar la suscripción, no para cobros a pacientes | Comprobado (precios) | https://odonto.ar/ |
| Admin: obras sociales / prepagas / aseguradoras y liquidaciones | Coberturas configurables y cálculo de parte de obra social y paciente; sin liquidaciones | Declarado | https://odonto.ar/ |
| Admin: reportes y métricas | Panel de finanzas; reportes no mencionados | Declarado | https://odonto.ar/ |
| Integraciones: WhatsApp Business API (oficial o no oficial) | No mencionado | No evidenciado | https://odonto.ar/ |
| Integraciones: Google Calendar / sitio web / API pública | Solo importación Excel; sin API ni Google Calendar | No evidenciado | https://odonto.ar/ |
| Integraciones: radiología, firma digital, marketing | No mencionado | No evidenciado | https://odonto.ar/ |
| Operación: sucursales, roles y permisos, auditoría | Control de acceso por roles (privacidad); historial de cambios en odontograma (declarado); sucursales no mencionadas | Comprobado (roles, privacidad); auditoría Declarado | https://odonto.ar/privacidad |
| Operación: exportación de datos | Copia de datos a pedido hasta 30 días tras la baja; sin exportación en cuenta activa descrita | Comprobado (términos) | https://odonto.ar/terminos |
| Operación: soporte y capacitación | Contacto por WhatsApp y correo; demo interactiva | Declarado | https://odonto.ar/ |
| Seguridad: datos de salud, privacidad, respaldo, control de acceso | Supabase con cifrado en reposo y tránsito, TLS, autenticación por token, seguridad por fila (RLS), roles, validación de sesiones concurrentes; respaldos no mencionados | Comprobado (política de privacidad) | https://odonto.ar/privacidad |
| Cumplimiento declarado (Ley 25.326, Ley 26.529, HC electrónica, HIPAA/GDPR, etc.) | Cita Ley 25.326 y AAIP; la clínica es responsable del tratamiento y Odonto.ar encargada; Ley 26.529 no mencionada | Comprobado (privacidad) | https://odonto.ar/privacidad |
| Comercial: prueba gratuita / plan gratuito | 30 días, 3 usuarios, todas las funciones, sin tarjeta | Comprobado (precios) | https://odonto.ar/ |
| Comercial: modalidad de cobro y costos adicionales visibles | Mensual en USD pagado en ARS por Mercado Pago al cambio del día; sin renovación automática; aviso de cambio de precio de 30 días; sin adicionales visibles | Comprobado (precios y términos) | https://odonto.ar/terminos |
| Adopción: casos de éxito, nº de clientes publicado, reseñas verificables | Ninguno publicado | No evidenciado | https://odonto.ar/ |

## Fortalezas, limitaciones y diferenciales evidentes

- **Fortalezas** (con evidencia): precios y límites de usuarios transparentes, con prueba de 30 días sin tarjeta (https://odonto.ar/); política de privacidad técnica concreta (https://odonto.ar/privacidad); autogestión de turnos del paciente en demo (https://odonto.ar/demo).
- **Limitaciones** (con evidencia o por ausencia de evidencia): sin WhatsApp, recordatorios, ARCA, lista de espera ni integraciones; exportación solo tras la baja; sin respaldos documentados; precio dolarizado (exposición cambiaria); sin clientes ni reseñas; razón social no identificada.
- **Diferencial real** (si lo hay): reserva online con identificación del paciente y precios públicos en USD con prueba larga, a costo bajo frente a los demás relevados.

## Puntuación preliminar (0-5)

Criterio: 0 = no evidenciado · 1-2 = básico o solo declarado · 3 = sólido y comprobado · 4-5 = superior y comprobado. Lo que solo esté "Declarado" no puede puntuar más de 2.

| Criterio | Peso | Puntaje | Justificación (1 línea) |
|---|---|---|---|
| Gestión de turnos y automatización | 25 % | 2 | Reserva online y duración por motivo vistas en demo; sin recordatorios ni automatización ni lista de espera. |
| Funcionalidad odontológica clínica | 20 % | 2 | Odontograma, HC y presupuestos solo declarados; sin periodontograma. |
| Integraciones locales y WhatsApp | 15 % | 1 | Solo Mercado Pago para suscripción; sin WhatsApp ni ARCA. |
| Administración, cobros y facturación | 15 % | 2 | Obras sociales y finanzas declaradas; sin facturación electrónica. |
| Experiencia del paciente | 10 % | 3 | Autogestión de turnos con validación de identidad comprobada en demo. |
| Seguridad, exportación y trazabilidad | 10 % | 3 | Controles técnicos concretos en privacidad; exportación limitada y sin respaldos documentados. |
| Precio y facilidad de adopción | 5 % | 4 | Precios públicos y prueba gratuita de 30 días sin tarjeta. |

**Total ponderado: 2,15 / 5**

## Fuentes consultadas

| URL | Tipo (oficial / precios / ayuda / video / marketplace / reseña) | Fecha de consulta |
|---|---|---|
| https://odonto.ar/ | oficial / precios | 2026-11-23 |
| https://odonto.ar/demo | oficial (demo interactiva) | 2026-11-23 |
| https://odonto.ar/terminos | oficial (términos) | 2026-11-23 |
| https://odonto.ar/privacidad | oficial (privacidad) | 2026-11-23 |
