# Tasks: agenda-turnos-core (C-10)

Convenciones de apply:
- **Governance CRÍTICO**: no se empieza ninguna tarea sin la aprobación explícita del usuario.
- **TDD estricto**: RED → GREEN → TRIANGULAR (caso feliz + borde) → REFACTOR, con los tests en verde tras cada paso.
- **Base real** con Testcontainers; cada test usa fechas futuras propias para no chocar en la base compartida.

## 1. Safety net

- [x] 1.1 Ejecutar `./gradlew build` sobre `main` antes de tocar nada y registrar "N/N tests verdes"; si algo falla, frenar y reportar. Verificación: baseline anotado en el resumen de apply.

## 2. Migración `turno`

- [x] 2.1 Crear `V7__turnos.sql` (D1) y actualizar `MigracionBaselineTest` a `"7:turnos"`. Verificación: `EsquemaTurnoTest` comprueba columnas, `CHECK` de estado, origen y cancelación, claves foráneas e índices.
- [x] 2.2 Probar la exclusion constraint y los permisos. Verificación: `EsquemaTurnoTest` rechaza dos turnos activos superpuestos; acepta contiguos, uno superpuesto si el otro está cancelado y uno marcado como sobreturno; rechaza un `CANCELADO` sin motivo; el runtime lee, inserta y actualiza pero no borra.

## 3. Dominio

- [x] 3.1 Modelar `EstadoTurno` con las transiciones de RN-TU-01 y `Turno` con confirmar, cancelar y reprogramar (D2, D6), más `TokenPublico` (D7). Verificación: `TurnoTest` unitario cubre cada transición válida e inválida, la duración mínima y máxima, el inicio en múltiplos de 5 y el token de 43 caracteres distinto en cada turno.

## 4. Persistencia y servicio

- [x] 4.1 Implementar el repositorio JDBC de turnos con el `UPDATE` con versión (D5) y la consulta del listado con JOIN (D8). Verificación: `RepositorioTurnosTest` guarda, lee, actualiza con la versión correcta, falla con una vieja y lista un rango ordenado con paciente y prestación.
- [x] 4.2 Implementar `ServicioDeTurnos` (crear, confirmar, cancelar, reprogramar, listar) con las reglas de D3, la traducción de D4, los eventos de D9 y la auditoría de D10. Verificación: `ServicioDeTurnosTest` con base real cubre alta con duración de la prestación y extendida, paciente o prestación inválidos, inicio pasado, superposición `409`, sobreturno, confirmación, cancelación, versión vieja y reprogramación (15 minutos sobre el mismo turno; a un horario ocupado deja el original intacto).
- [x] 4.3 Probar la concurrencia y los eventos. Verificación: `ConcurrenciaTurnosTest` (dos hilos, uno gana) y `EventosDeTurnosTest` (un oyente `AFTER_COMMIT` recibe `TurnoCreado`, `TurnoCancelado`, `TurnoReprogramado` y `TurnoEstadoCambiado`, y nada si la creación falla).

## 5. API

- [x] 5.1 Implementar `TurnosController` (D11). Verificación: `TurnosControllerTest` (MockMvc, seguridad real) cubre `POST` `201` con `version` y token, `400` por campo, `409` superpuesto con `type` y sugerencia de sobreturno, `GET` por rango (`400` si supera 31 días), `GET` por id y `404`, `PUT` reprogramar, `cancelar` sin motivo `400`, `confirmar`, versión vieja `409`, secretaria con acceso completo y `401` sin sesión.

## 6. Verificación integral

- [x] 6.1 Ejecutar `./gradlew build` completo: todos los tests en verde, JaCoCo ≥ 80 % de líneas y ArchUnit sin violaciones. Verificación: totales en el resumen de apply.
- [x] 6.2 Levantar el stack con `docker compose` y recorrer con `curl`: login, alta de paciente, turno para mañana, turno superpuesto `409`, sobreturno, listado del día, reprogramación, cancelación y auditoría. Verificación: secuencia y códigos anotados en el resumen de apply.

## Workflow follow-up

- Revisión humana del PR (CRÍTICO), merge, sincronizar `openspec/specs/agenda-turnos`, archivar y marcar C-10 en `CHANGES.md`.
- Proponer aparte el script de datos de demo (pacientes y turnos ficticios).
