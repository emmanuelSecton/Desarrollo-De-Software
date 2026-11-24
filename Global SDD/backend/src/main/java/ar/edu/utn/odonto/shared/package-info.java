/**
 * Modulo transversal {@code shared}: lo usan todos los modulos y no depende de ninguno.
 *
 * <h2>Convencion de tiempo (RN-GL-02)</h2>
 *
 * <ul>
 *   <li>Todo instante se guarda en columnas {@code TIMESTAMPTZ} (en UTC) y en Java se representa con
 *       {@link java.time.Instant} u {@link java.time.OffsetDateTime}. Nunca {@code TIMESTAMP} sin zona
 *       ni {@code LocalDateTime} para persistir instantes.
 *   <li>La hora actual se obtiene SIEMPRE del {@link java.time.Clock} inyectado (UTC). Nadie llama a
 *       {@code now()} sin argumentos: una regla de ArchUnit rompe el build. Los tests fijan el reloj con
 *       {@code Clock.fixed(...)}.
 *   <li>La zona del consultorio ({@code APP_TIMEZONE}, por defecto {@code America/Argentina/Buenos_Aires})
 *       se usa solo para mostrar y para interpretar fechas ingresadas por el usuario, mediante
 *       {@code ZonaHorariaConsultorio}.
 *   <li>La API serializa instantes en ISO-8601 con desplazamiento (por ejemplo
 *       {@code 2026-11-24T13:00:00Z}), nunca como numeros.
 * </ul>
 *
 * <h2>Otras convenciones</h2>
 *
 * <ul>
 *   <li>Los endpoints REST se publican bajo {@code Api.V1} ({@code /api/v1}).
 *   <li>Los errores salen como {@code ProblemDetail} (RFC 7807) por el unico
 *       {@code ManejadorGlobalDeErrores}; sin datos clinicos ni personales en logs (Ley 25.326).
 * </ul>
 */
package ar.edu.utn.odonto.shared;
