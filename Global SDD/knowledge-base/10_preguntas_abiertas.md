# Preguntas Abiertas

## Inconsistencias detectadas

### IN-01 — Producto para muchos clientes vs. instancia por consultorio ✅ Resuelta (2026-11-24)
**Discovery dice**: producto real a lanzar, vendido a muchos odontólogos independientes.
**Decisión técnica original dice**: una instancia completa por consultorio (servidor y base gestionada propios).
**Impacto**: el costo de operación crecía de forma lineal y contradecía la prioridad de costo.
**Resolución**: DD-01 revisada. La aplicación sigue siendo single-tenant, con un contenedor y una base por consultorio, pero sobre **host y clúster PostgreSQL compartidos**. Baja el costo marginal sin cambiar el código. Riesgos residuales en R-01; se revisa a los ~30 consultorios.

### IN-02 — Diferencial anti-ausentismo vs. reglas conservadoras ✅ Resuelta (2026-11-24)
**Discovery dice**: el diferencial es el anti-ausentismo.
**Reglas originales decían**: solo aviso a 24 h, lista de espera manual, sin seña.
**Impacto**: el MVP quedaba funcionalmente parecido a Bilog o Dentalink.
**Resolución**: DD-10. **Oferta de huecos por WhatsApp con un clic**: la secretaria elige hasta 5 pacientes de la lista de espera y gana el primero que toca "Lo tomo". Es algo que ningún producto argentino relevado evidencia, mantiene el control humano y reutiliza WhatsApp y la exclusion constraint. Nuevas reglas RN-LE-04 a RN-LE-07, historia US-033 y riesgo R-06.

## Preguntas abiertas (priorizadas)

| ID | Prioridad | Pregunta | Bloquea | Decisor |
|---|---|---|---|---|
| PA-01 | ✅ Resuelta (2026-11-24) | **1-2 personas, ~3 meses.** Se recortan los changes de prioridad S (C-30, C-31, C-34) y se replanifica para 2 personas (DD-13). | — | Product Owner |
| PA-02 | ✅ Resuelta (2026-11-24) | **TypeScript solo en el frontend**; backend 100 % Java/Spring Boot (SU-02 confirmado). | — | Product Owner |
| PA-03 | ✅ Resuelta para la demo (2026-11-24) | **Simplicidad y bajo costo para una demo**: un VPS económico con Docker Compose (DD-11). El hosting de producción queda en PA-13. | — | Equipo técnico |
| PA-13 | Media | ¿Qué hosting y PostgreSQL gestionado para producción (región, alta disponibilidad, cumplimiento)? Ver DD-01 y DD-11. | Primer cliente real | Equipo técnico |
| PA-04 | ✅ Resuelta (2026-11-24) | **Cada odontólogo** da de alta su Meta Business, su WABA y su número, con una guía paso a paso, y carga las credenciales en el panel (DD-12). | — | Product Owner |
| PA-05 | Media | ¿Alcanza con DNI + celular para reservar, o hace falta verificar el celular con un código (OTP)? | Reserva online | Product Owner |
| PA-06 | Media | ¿Se necesitan sobreturnos en el MVP (RN-AG-07)? | Agenda | Odontólogos (entrevistas) |
| PA-07 | Media | ¿Cuál es el precio y el modelo de cobro (mensual en ARS, prueba gratuita)? ¿Cubre el costo por instancia? | Lanzamiento | Product Owner |
| PA-08 | Media | ¿Qué catálogo de estados del odontograma y qué convención de colores usar (por ejemplo, rojo a realizar y azul realizado)? | US-042 | Odontólogo asesor |
| PA-09 | Media | ¿Dónde se alojan los datos? ¿Hace falta que estén en Argentina, o alcanza con un país con nivel adecuado de protección? | Hosting, Ley 25.326 | Asesor legal |
| PA-10 | Baja | ¿Qué proveedor de email transaccional? | Notificaciones | Equipo técnico |
| PA-11 | ✅ Resuelta (2026-11-26) | **Odontogestión.** El isotipo de diente de `design-system/assets/logo.svg` queda como logo definitivo (revisión 2 del sistema de diseño). | — | Product Owner |
| PA-12 | Baja | ¿Hace falta firma digital del profesional en las entradas de historia clínica (Ley 25.506) o alcanza con usuario autenticado + hash? | Cumplimiento | Asesor legal |
