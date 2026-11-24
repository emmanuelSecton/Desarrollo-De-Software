# Estudio de mercado: software odontológico de turnos y agenda (Argentina y LatAm)

- **Fecha de consulta de todas las fuentes**: 2026-11-23
- **Sistemas relevados**: 20 (11 argentinos, 5 latinoamericanos con presencia o vertical para Argentina, 4 internacionales de referencia)
- **Evidencia de respaldo**: una ficha por sistema en [`sources/`](sources/), con cada afirmación clasificada y su URL. Las fichas siguen la plantilla [`_plantilla-relevamiento.md`](_plantilla-relevamiento.md).

## Método y advertencias

1. **Niveles de evidencia.** Cada funcionalidad se clasificó como:
   - **Comprobado**: aparece en la documentación pública, el centro de ayuda, la página de precios, una demo pública o los términos legales.
   - **Declarado**: solo aparece como afirmación comercial en la landing.
   - **No evidenciado**: no se encontró ninguna mención pública.
2. **Regla de puntuación.** Una funcionalidad solo declarada no puede puntuar más de 2 sobre 5. Por eso los puntajes son bajos en general: **miden evidencia pública, no calidad real del producto.** Un sistema maduro que no publica documentación, como Dentalink o Curve, queda penalizado. Esa brecha es la razón de la demo recomendada en la sección D.
3. **Herramienta de lectura.** La lectura se hizo con un lector web automatizado, que resume las páginas. No se recorrieron demos con usuario, apps móviles ni videos de YouTube. Capterra y GetApp bloquearon el acceso (HTTP 403) y no se intentó evadir ese bloqueo. **No se obtuvieron reseñas verificadas de marketplaces**, salvo Dentalink (2 reseñas en Capterra) y los perfiles públicos de Doctoralia.
4. **Odontiti** se mantiene en la lista, pero su sitio no devolvió contenido legible. Su puntaje de 0 indica falta de evidencia, no una evaluación del producto, y se recomienda revisarlo a mano.
5. **Precios.** Se transcriben en la moneda publicada. Los importes en ARS cambian con frecuencia y deben revalidarse antes de cualquier decisión.

---

## A. Tabla comparativa (ordenada por relevancia para Argentina)

La relevancia combina presencia en Argentina, integraciones locales (ARCA, obras sociales, Mercado Pago, WhatsApp) y solidez de la evidencia.

| # | Sistema | Proveedor · origen | Segmento | Modalidad | WhatsApp | ARCA | Obras sociales | Precio de entrada publicado | Prueba / gratis | Evidencia de adopción |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | [Órbita](sources/orbita.md) | Órbita Global · AR (La Plata) | Consultorio a red multisede | SaaS (AWS) | Asistente IA; API oficial **declarada** | **Comprobado** (ayuda, incl. nota de crédito) | **Comprobado** (lote mensual) | No publicado (a cotizar) | Bonificaciones, sin prueba general | ~12 logos, prensa 07/2026; declarado |
| 2 | [Bilog](sources/bilog.md) | Bilog · AR | Consultorio, clínica, obras sociales, universidades | Web + escritorio + app (declarado) | Enlace click-to-chat, envío manual; packs pagos | No evidenciado | **Comprobado** (liquidación y auditoría) | Freemium gratis (1 prof., 50 pac.); pagos sin precio | Freemium permanente | "+1.500 clínicas" declarado |
| 3 | [AgendaPro](sources/agendapro.md) | AgendaPro · CL | Independiente a multisede (horizontal) | SaaS + app iOS/Android (comprobado) | Oficial, indicio fuerte (Meta cobra) | "Próximamente" | No evidenciado | ARS 9.900/mes (IVA incl.) | Sí | "+30.000 clínicas" declarado |
| 4 | [Oclusar](sources/oclusar.md) | Oclusar · AR (Córdoba) | Consultorio y clínica; alquiler de sillón | SaaS web móvil | Click-to-chat oficial, manual (comprobado) | Declarado | Seguimiento declarado | No publicado (ARS) | Plan inicial gratis (declarado) | No publicada |
| 5 | [Dentia](sources/dentia.md) | Persona humana · AR (Villa María) | Consultorio a multisede | SaaS web + app instalable | **Cloud API oficial** (términos) | Declarado | Liquidaciones declaradas | $20.000/mes (pesos) | 30 días (declarado) | "+100 clínicas" declarado |
| 6 | [OCLÜ](sources/oclu.md) | OCLÜ · AR / ES | Consultorio y clínica por boxes | Web + app (declarado) | Add-on, tipo no informado | Add-on declarado | Parcial declarado | ARS 35.000 + IVA/mes | Sí | No publicada |
| 7 | [Odonto.ar](sources/odonto-ar.md) | Odonto.ar · AR (Córdoba) | Independiente y clínica pequeña | SaaS web (Supabase) | No evidenciado | No evidenciado | Coberturas declaradas | USD 29/mes (pago en ARS vía MP) | 30 días sin tarjeta | No publicada |
| 8 | [Dentalink](sources/dentalink.md) | Engenis SpA · CL | Independiente a multisucursal | SaaS (AWS) | Vía add-on Contact Center IA | No evidenciado | **Comprobado** (planes) | No publicado (solo promo IA USD 50) | No | "15.000 clínicas" declarado; Capterra 2 reseñas |
| 9 | [Dentiqa](sources/dentiqa.md) | Violet Wave · AR | Clínica a cadena | SaaS web (Supabase, São Paulo) | **Cloud API oficial** (privacidad) | No evidenciado | No evidenciado | USD 89/mes (cobro en ARS) | Contradictorio | Ninguna (cupo fundadores) |
| 10 | [ClinIA](sources/clinia.md) | EMPRINET 4.0 · AR (Córdoba) | Salud pública, clínicas, cadenas (generalista) | Web, instancia por cliente | Asistente, tipo no informado | Declarado | Declarado (PAMI, SUMAR+, PUCO) | No publicado | No | "+500 centros" declarado |
| 11 | [Doctoralia](sources/doctoralia.md) | Docplanner · ES | Profesional que busca pacientes (directorio) | SaaS + directorio | Declarado (precios listan SMS/email) | No evidenciado | Solo filtro de cobertura | ARS 25.000/mes (anual) | Versión gratuita mencionada | **Comprobado**: miles de perfiles con reseñas |
| 12 | [Reservo](sources/reservo.md) | No evidenciado · CL | Consultorio y centro multiespecialidad | SaaS | Declarado (terceros) | Landing AR, sin detalle | Landing AR, sin detalle | Solo CLP vía terceros | No evidenciado | "4.000 clientes" (tercero) |
| 13 | [OdontoGRAMA](sources/odontograma.md) | Solsoftware · AR (indicios) | Consultorio | Nube (declarado) | No evidenciado | No evidenciado | Facturación declarada | No publicado | Cuenta gratuita declarada | No publicada |
| 14 | [OdontLux](sources/odontlux.md) | Creative Software · AR (indicios) | Consultorio y clínica | Nube (declarado) | No evidenciado | No evidenciado | No evidenciado | No publicado | No | No publicada |
| 15 | [SMILE](sources/smile.md) | SmileApp LLC · EE. UU. (foco LatAm) | Independiente a multisucursal | SaaS / PWA (Google Cloud) | API oficial declarada (add-on) | No evidenciado; **AR no figura entre sus países** | No evidenciado | USD 19/mes | 30 días sin tarjeta | Ninguna |
| 16 | [Odontosist](sources/odontosist.md) | No evidenciado · PE | Independiente a red | SaaS web | Recordatorios (guía), tipo no informado | No (tampoco SUNAT) | No | S/ 39–55/mes | 14 días sin tarjeta | Ninguna |
| 17 | [Clinic Cloud](sources/clinic-cloud.md) | Doctoralia España SL · ES | Multiespecialidad, odontograma como módulo | SaaS | Recordatorios (ayuda), tipo no informado | No (Verifactu/TicketBAI) | Aseguradoras "próximamente" | 29 € + IVA/mes | Sí, sin permanencia | "+12.000 doctores" declarado |
| 18 | [Dentally](sources/dentally.md) | Henry Schein One · UK | Independiente a grupos | SaaS (AWS UE) | No (SMS/email) | No (NHS, CDAnet) | Equivalentes locales | £134/mes sin IVA | No publicada | Testimonios |
| 19 | [Curve Dental](sources/curve-dental.md) | CD Newco LLC · EE. UU. | Independiente a DSO | SaaS (AWS) | No (SMS) | No (eClaims, HIPAA) | Seguros EE. UU./Canadá | No publicado | No | "100.000+ usuarios" declarado |
| 20 | [Odontiti](sources/odontiti.md) | No evidenciado | No evidenciado | No evidenciado | No evidenciado | No evidenciado | No evidenciado | No accesible | No evidenciado | Ninguna |

---

## B. Matriz de puntuación (0–5, ponderada)

Pesos: **Turnos y automatización** 25 % · **Clínica odontológica** 20 % · **Integraciones locales y WhatsApp** 15 % · **Administración, cobros y facturación** 15 % · **Experiencia del paciente** 10 % · **Seguridad, exportación y trazabilidad** 10 % · **Precio y adopción** 5 %. La justificación de cada celda está en la ficha del sistema.

| Sistema | Turnos 25 % | Clínica 20 % | Integr. 15 % | Admin. 15 % | Paciente 10 % | Seguridad 10 % | Precio 5 % | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| Órbita | 3 | 3 | 3 | 3 | 2 | 3 | 1 | **2,80** |
| Bilog | 3 | 3 | 2 | 3 | 2 | 2 | 3 | **2,65** |
| Clinic Cloud | 3 | 2 | 2 | 3 | 3 | 2 | 4 | **2,60** |
| AgendaPro | 3 | 2 | 3 | 2 | 2 | 1 | 4 | **2,40** |
| Oclusar | 2 | 2 | 3 | 2 | 2 | 4 | 3 | **2,40** |
| Odontosist | 3 | 3 | 2 | 2 | 2 | 1 | 3 | **2,40** |
| Dentia | 2 | 2 | 3 | 2 | 2 | 3 | 4 | **2,35** |
| Odonto.ar | 2 | 2 | 1 | 2 | 3 | 3 | 4 | **2,15** |
| OCLÜ | 2 | 2 | 2 | 2 | 2 | 2 | 4 | **2,10** |
| Dentally | 2 | 2 | 2 | 2 | 2 | 3 | 2 | **2,10** |
| Dentalink | 2 | 3 | 1 | 2 | 2 | 2 | 1 | **2,00** |
| Dentiqa | 2 | 2 | 2 | 1 | 2 | 3 | 2 | **1,95** |
| Curve Dental | 2 | 2 | 2 | 2 | 2 | 2 | 1 | **1,95** |
| ClinIA | 2 | 2 | 2 | 2 | 2 | 1 | 1 | **1,85** |
| Reservo | 2 | 2 | 2 | 2 | 2 | 1 | 1 | **1,85** |
| SMILE | 2 | 2 | 2 | 1 | 1 | 2 | 4 | **1,85** |
| Doctoralia | 3 | 1 | 1 | 0 | 4 | 1 | 2 | **1,70** |
| OdontoGRAMA | 1 | 1 | 0 | 1 | 0 | 1 | 2 | **0,80** |
| OdontLux | 1 | 1 | 0 | 1 | 1 | 0 | 1 | **0,75** |
| Odontiti | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **0,00** |

**Lectura crítica**
- **Ningún sistema supera 3/5 con evidencia pública.** El techo está en 2,80, de Órbita, que es justamente el que más documenta en un centro de ayuda. El mercado argentino comunica funcionalidades, pero casi no las demuestra.
- La **diferencia entre los primeros puestos es pequeña** (2,80 contra 2,40). Con este nivel de evidencia, el orden entre los puestos 1 y 9 no es concluyente: hay que validarlo con demos.
- Los internacionales (Dentally, Curve) puntúan bajo por **no aplicar al contexto local**, no por debilidad de producto. Su valor es de referencia (sección D).

---

## C. Análisis competitivo

### C.1 Funcionalidades ya estándar en el mercado (la línea base)
Casi todos los sistemas las ofrecen o declaran. No diferencian, pero su ausencia descalifica.
- Agenda en la nube con varios profesionales y vista diaria o semanal.
- Historia clínica con **odontograma** y **presupuestos**.
- Recordatorios de turno, cada vez más por WhatsApp.
- Caja y cobros básicos.
- Acceso desde el celular, sea por web adaptable o por app.
- Algún tipo de enlace para que el paciente pida turno.

### C.2 Diferenciales reales (con evidencia)
| Diferencial | Quién lo tiene con evidencia | Por qué importa |
|---|---|---|
| Facturación **ARCA** integrada y documentada, con nota de crédito | Órbita (ayuda) | Es el mayor dolor administrativo local; los demás solo la declaran o la tienen "próximamente" (AgendaPro). |
| **Liquidación a obras sociales** y auditoría | Bilog, Órbita, Dentalink (planes) | Rasgo específico de Argentina que los productos regionales no resuelven. |
| **WhatsApp Cloud API oficial** respaldada por un documento legal | Dentia (términos), Dentiqa (privacidad); AgendaPro (indicio) | Evita los bloqueos de las soluciones no oficiales. Oclusar eligió click-to-chat oficial manual por la misma razón. |
| **Marco legal y de datos explícito** (subprocesadores, DPA, exportación, Ley 25.326 / 26.529) | Oclusar, Dentia, Dentiqa, Odonto.ar | Los datos de salud son sensibles; poder demostrar cumplimiento genera confianza y es poco común. |
| **Precios publicados y prueba sin tarjeta** | AgendaPro, Odonto.ar, OCLÜ, Dentia, SMILE | Baja la fricción de adopción en un mercado donde la mitad solo "cotiza". |
| **Captación de pacientes** (directorio con reseñas) | Doctoralia | Es otro negocio (marketing), complementario a la gestión. |

### C.3 Vacíos frecuentes del mercado argentino
1. **Lista de espera** para cubrir cancelaciones: **no evidenciada en ningún producto argentino.** Solo aparece en Doctoralia (plan VIP) y declarada en SMILE y Dentally.
2. **Prevención de ausentes de punta a punta**: el recordatorio existe, pero no el circuito completo de confirmar, liberar el turno y ofrecerlo a la lista de espera. Varios recordatorios argentinos son **manuales** (Bilog, Oclusar).
3. **Sillones o boxes como recurso agendable**: solo aparecen en algunos (OCLÜ por plan, Órbita por sillón). En el resto la agenda es por profesional.
4. **Señas online con Mercado Pago para reservar el turno**: declaradas en pocos casos (Oclusar, Dentia) y sin comprobación.
5. **Exportación de datos y portabilidad**: rara vez documentada, salvo Oclusar, Dentia, SMILE y Órbita (a Excel). Genera dependencia del proveedor.
6. **Transparencia de precios**: cerca de la mitad de los 20 no publica precio oficial. Los add-ons críticos (WhatsApp, ARCA, autoagenda) suelen cobrarse aparte y sin precio visible (OCLÜ, Bilog, Dentalink).
7. **Documentación pública y reseñas verificables**: prácticamente inexistentes en los productos locales, que en su mayoría son jóvenes (2023–2026).

### C.4 Oportunidades de innovación
- **Circuito anti-ausentismo completo**: confirmación por WhatsApp oficial; si el paciente no confirma, el turno se libera y se ofrece en orden a la lista de espera. Es el vacío más claro y el de mayor impacto económico directo para el consultorio.
- **Seña con Mercado Pago al reservar**, configurable por prestación (por ejemplo, solo para turnos largos o de pacientes con ausencias previas).
- **Agenda por profesional y por sillón a la vez**, con duración por prestación y prevención de solapamientos.
- **"Paquete argentino" desde el plan base**: ARCA, obras sociales y Mercado Pago. Seguiría el modelo de localización de Dentally, sin cobrarlos como add-ons ocultos.
- **Transparencia como posicionamiento**: precio en ARS publicado, prueba sin tarjeta, exportación completa y documentación pública de cumplimiento (Ley 25.326 y Ley 26.529).
- **IA conversacional con derivación a humano**: Órbita, Dentia, Dentiqa y SMILE ya la declaran, así que **no es diferencial por sí sola**. Lo diferencial sería que funcione con evidencia y a un costo previsible.

---

## D. Recomendación final

### D.1 Cinco competidores prioritarios para analizar mediante demo
| # | Sistema | Qué validar en la demo |
|---|---|---|
| 1 | **Órbita** | Bot de WhatsApp (¿es realmente API oficial?), facturación ARCA, liquidación a obras sociales y precio real. Es el más completo en evidencia. |
| 2 | **Bilog** | Profundidad en obras sociales y auditoría; si los recordatorios son automáticos o manuales; precios de Lite y Premium. Es el más establecido localmente. |
| 3 | **AgendaPro** | Fortaleza en turnos y automatización con precios en ARS; madurez de la vertical dental (odontograma) y fecha real de ARCA. |
| 4 | **Dentalink** | Escala regional y funcionamiento de su Contact Center IA; si en Argentina tiene facturación electrónica y Mercado Pago. |
| 5 | **Dentia** | Las funciones declaradas (casi todas) y la robustez operativa de un proveedor unipersonal. Es la propuesta más parecida en precio y alcance a un MVP local. |

### D.2 Tres productos de referencia para experiencia de usuario
1. **Dentally**: portal del paciente (reserva, formularios previos, pago) y modelo de localización por país.
2. **Doctoralia**: reserva 24/7 y confianza mediante perfiles con reseñas. Es la experiencia del paciente mejor comprobada del relevamiento.
3. **AgendaPro**: incorporación autoservicio con precios claros, app móvil del profesional y flujo de reserva multicanal.

### D.3 MVP sugerido

**Imprescindibles (v1)**
- Agenda por profesional y por sillón, con vista diaria y semanal, duración por prestación, bloqueos, sobreturnos controlados y prevención de solapamientos.
- Reserva online del paciente por enlace o web 24/7, con identificación del paciente, cancelación y reprogramación.
- Recordatorios y confirmaciones automáticos por **WhatsApp Cloud API oficial**, con respaldo por email.
- Ficha del paciente: datos, anamnesis, **odontograma** y evolución.
- Presupuestos simples y registro de cobros (caja básica).
- Roles y permisos (odontólogo, recepción, administrador), registro de auditoría y **exportación de datos**.
- Cumplimiento declarado y documentado: Ley 25.326, Ley 26.529, política de privacidad y lista de subprocesadores.

**Diferenciadores (v1 o v1.1)**
- **Lista de espera inteligente**: un turno liberado se ofrece automáticamente por WhatsApp.
- **Seña por Mercado Pago** al reservar, configurable por prestación.
- Panel de ausentismo: tasa de ausencias, turnos recuperados e ingresos protegidos.
- Precio publicado en ARS y prueba sin tarjeta.

**Para etapas posteriores**
- Facturación electrónica ARCA. Es importante pero de integración compleja; se puede cubrir antes exportando a un sistema contable.
- Liquidación a obras sociales y prepagas, con nomencladores.
- Periodontograma, imágenes o DICOM y radiología.
- Planes de tratamiento por etapas, campañas de recall y controles periódicos.
- Multisucursal, API pública, firma digital y recetas electrónicas.
- Asistente conversacional con IA.

---

## Fuentes

Las URL, la fecha de consulta y la clasificación de cada afirmación están en la sección **Fuentes consultadas** de cada ficha en [`sources/`](sources/). La búsqueda inicial de candidatos (2026-11-23) partió de buscadores web y de la guía comparativa de Reservo: https://reservo.cl/blog/profesionales/dental/guia-para-elegir-el-mejor-software-dental-en-argentina/
