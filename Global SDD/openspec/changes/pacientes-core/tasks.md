# Tasks: pacientes-core (C-09)

Convenciones de apply:
- **Governance ALTO**: no se empieza ninguna tarea sin la aprobación del usuario sobre proposal, specs y design.
- **TDD estricto**: RED → GREEN → TRIANGULAR (caso feliz + borde) → REFACTOR, con los tests en verde tras cada paso.
- **Base real**: Testcontainers con PostgreSQL 16, nunca H2 ni mocks de la base.
- **Datos ficticios** (SU-07): DNIs y teléfonos inventados, emails `*@consultorio.test`.

## 1. Safety net y dependencias

- [x] 1.1 Ejecutar `./gradlew build` sobre `main` antes de tocar nada y registrar "N/N tests verdes"; si algo falla, frenar y reportar. Verificación: baseline anotado en el resumen de apply.
- [x] 1.2 Agregar `com.googlecode.libphonenumber:libphonenumber` con versión fija. Verificación: `./gradlew dependencies` la resuelve y el build sigue verde.

## 2. Migración `paciente`

- [x] 2.1 Crear `V4__pacientes.sql` con `unaccent`, la tabla de D1, sus `CHECK`, los índices y `GRANT SELECT, INSERT, UPDATE ON paciente TO ${runtime_role}`; no tocar migraciones anteriores. Actualizar `MigracionBaselineTest` a `["1:baseline", "2:auditoria", "3:identidad", "4:pacientes"]`. Verificación: `EsquemaPacienteTest` comprueba tipos, ausencia de `DEFAULT now()` en las fechas e índices; `MigracionBaselineTest` verde.
- [x] 2.2 Probar las garantías de base. Verificación: `EsquemaPacienteTest` rechaza DNI repetido, DNI con letras o de 6 dígitos, teléfono fuera de E.164, `ausencias_count` negativo y `acepta_whatsapp = true` sin fecha; el rol runtime lee, inserta y actualiza pero no borra.

## 3. Dominio, normalización y persistencia

- [x] 3.1 Implementar `NormalizadorDeTelefono` (D3). Verificación: `NormalizadorDeTelefonoTest` convierte `011 15-2345-6789`, `+54 9 11 2345-6789`, `1123456789`, `(0351) 15 555-1234` y `+5493515551234` al E.164 de celular correcto, y rechaza `12345`, texto y números vacíos.
- [x] 3.2 Modelar `Paciente` en `domain` con la normalización de DNI (quitar puntos y espacios) y las reglas de consentimiento de WhatsApp de D7. Verificación: `PacienteTest` unitario cubre DNI con puntos, alta con y sin WhatsApp, revocación y reactivación del consentimiento.
- [x] 3.3 Implementar el puerto `RepositorioPacientes` y `RepositorioPacientesJdbc`, incluida la búsqueda de D5 y el `INSERT … ON CONFLICT` de D6. Verificación: `RepositorioPacientesTest` con Testcontainers guarda y lee, busca por apellido sin acentos (`perez` → "Pérez"), por nombre y apellido en cualquier orden, por DNI, por teléfono y excluye inactivos salvo pedido explícito.

## 4. Servicios de aplicación

- [x] 4.1 Implementar `RegistroDePacientes` (alta, modificación, baja, reactivación, obtener o crear) con la auditoría de D9 y la configuración `pacientes.politica-privacidad-version`. Verificación: `RegistroDePacientesTest` con base real cubre el alta con evento `CREAR_ENTIDAD` sin datos personales en ninguna columna de `evento_auditoria`, el DNI duplicado sin evento, la modificación que conserva `consentimiento_datos_at` y `ausencias_count`, la baja y la reactivación.
- [x] 4.2 Probar "obtener o crear" (D6). Verificación: `ObtenerOCrearPacienteTest` devuelve el existente sin cambiar su celular, crea uno nuevo con un único evento, y con dos hilos simultáneos sobre el mismo DNI nuevo deja un solo paciente y ambos reciben el mismo id.

## 5. API REST y permisos

- [x] 5.1 Implementar `PacientesController` con los DTOs y errores de D4. Verificación: `PacientesControllerTest` (MockMvc, seguridad real) cubre alta `201`, alta duplicada `409` con `pacienteId`, alta sin consentimiento `400`, DNI con letras, email inválido, fecha futura y teléfono inválido `400` por campo, consulta `200`, id inexistente `404`, modificación `200` con teléfono normalizado y cambio de DNI a uno existente `409`.
- [x] 5.2 Implementar la búsqueda paginada. Verificación: `BusquedaPacientesTest` cubre texto sin acentos, DNI, teléfono, `incluirInactivos`, `tamano` máximo 100 y `total` correcto.
- [x] 5.3 Aplicar los permisos de D8. Verificación: `PermisosPacientesTest` confirma que la secretaria busca, da de alta y modifica; recibe `403` en baja y reactivación sin cambiar nada; el odontólogo da de baja (`204`) y reactiva; sin sesión todo responde `401`.
- [x] 5.4 Documentar `POLITICA_PRIVACIDAD_VERSION` en `deploy/docker-compose.yml` y en el README. Verificación: `ConfiguracionFlywayComposeTest` sigue verde y el README nombra la variable.

## 6. Verificación integral

- [x] 6.1 Ejecutar `./gradlew build` completo: todos los tests en verde, JaCoCo ≥ 80 % de líneas y ArchUnit sin violaciones. Verificación: totales de tests y cobertura en el resumen de apply.
- [x] 6.2 Levantar el stack con `docker compose` en una base vacía y recorrer con `curl`: login, alta de un paciente, búsqueda sin acentos, modificación, baja como odontólogo y la auditoría resultante. Verificación: secuencia y códigos de estado anotados en el resumen de apply.

## Workflow follow-up

- Agregar `POLITICA_PRIVACIDAD_VERSION` a `.env.example` (el agente no tiene permiso sobre ese archivo).
- Revisión del PR, merge, sincronizar `openspec/specs/pacientes` y archivar; marcar C-09 en `CHANGES.md`.
