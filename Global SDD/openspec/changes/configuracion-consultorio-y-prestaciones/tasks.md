# Tasks: configuracion-consultorio-y-prestaciones (C-05)

Convenciones de apply:
- **Governance MEDIO**: se implementa con puntos de control; cualquier decisión nueva se muestra al usuario.
- **TDD estricto**: RED → GREEN → TRIANGULAR (caso feliz + borde) → REFACTOR, con los tests en verde tras cada paso.
- **Base real** con Testcontainers. Los tests que cambian el consultorio o las franjas iniciales las restauran al terminar.

## 1. Safety net

- [x] 1.1 Ejecutar `./gradlew build` sobre `main` antes de tocar nada y registrar "N/N tests verdes"; si algo falla, frenar y reportar. Verificación: baseline anotado en el resumen de apply.

## 2. Migración y datos iniciales

- [x] 2.1 Crear `V6__configuracion_consultorio.sql` con las tres tablas (D3, D4, D5), sus `CHECK`, la exclusion constraint, el índice único parcial, los grants del runtime y los datos iniciales (D6). Actualizar `MigracionBaselineTest` a `"6:configuracion consultorio"`. Verificación: `EsquemaConfiguracionTest` comprueba los datos iniciales (1 consultorio con 48/24/24/30, 10 franjas, 6 prestaciones).
- [x] 2.2 Probar las garantías de base. Verificación: `EsquemaConfiguracionTest` rechaza un segundo consultorio, confirmación mayor o igual al recordatorio, slug inválido, franja con inicio mayor al fin, franja de 09:07, franja superpuesta del mismo día, duración 0 o 32, y dos prestaciones activas con el mismo nombre; acepta franjas contiguas, la misma franja otro día y un nombre repetido si la otra está inactiva. El runtime no puede insertar ni borrar consultorios ni borrar prestaciones.

## 3. Dominio, persistencia y consulta

- [x] 3.1 Modelar `Consultorio`, `FranjaDeAtencion` y `Prestacion` en `agenda.domain` con sus validaciones. Verificación: tests unitarios de cada regla (rangos de parámetros, confirmación menor que recordatorio, múltiplos de 5, inicio menor que fin).
- [x] 3.2 Implementar los puertos y repositorios JDBC, y `ConfiguracionDeAgenda` (D1). Verificación: `RepositoriosConfiguracionTest` con Testcontainers lee y guarda cada entidad, lista franjas ordenadas por día y hora, y `ConfiguracionDeAgenda` devuelve los parámetros y una prestación por id.

## 4. Servicios y API

- [x] 4.1 Implementar el servicio y `ConsultorioController` (`GET`/`PUT`) con la auditoría de D8. Verificación: `ConsultorioControllerTest` cubre `GET` `200`, `PUT` válido `200` con evento `MODIFICAR_ENTIDAD`, slug inválido `400` y confirmación mayor o igual al recordatorio `400` sin cambios.
- [x] 4.2 Implementar `HorariosController`. Verificación: `HorariosControllerTest` cubre listado ordenado, alta `201`, alta superpuesta `409` con `type` propio, contigua `201`, modificación `200`, borrado `204` con evento, inexistente `404` y franja inválida `400`.
- [x] 4.3 Implementar `PrestacionesController`. Verificación: `PrestacionesControllerTest` cubre listado de activas y con `incluirInactivas`, alta `201`, duración inválida `400`, nombre repetido `409`, modificación `200`, baja `204` y reactivación `204`.
- [x] 4.4 Aplicar los permisos de D7. Verificación: `PermisosConfiguracionTest` confirma que la secretaria lee todo (`200`) y recibe `403` en cada escritura sin cambios, y que sin sesión todo responde `401`.

## 5. Verificación integral

- [x] 5.1 Ejecutar `./gradlew build` completo: todos los tests en verde, JaCoCo ≥ 80 % de líneas y ArchUnit sin violaciones. Verificación: totales en el resumen de apply.
- [x] 5.2 Levantar el stack con `docker compose` en una base vacía y recorrer con `curl` como odontólogo: leer la configuración inicial, completar los datos del consultorio, crear una franja superpuesta (`409`) y una válida, dar de alta y de baja una prestación, y revisar la auditoría. Verificación: secuencia y códigos anotados en el resumen de apply.

## Workflow follow-up

- Revisión del PR, merge, sincronizar `openspec/specs/configuracion-consultorio`, archivar y marcar C-05 en `CHANGES.md`.
