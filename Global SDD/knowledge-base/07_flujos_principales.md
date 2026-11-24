# Flujos Principales

## Flujo 1: Reserva online del paciente

**Disparador**: el paciente abre el enlace público del consultorio.
**Actor**: paciente.

**Pasos**:
1. La SPA pide `GET /public/disponibilidad?prestacion&desde`.
2. El servicio de agenda calcula los huecos: `horario_atencion` menos bloqueos, menos turnos activos, menos anticipación mínima (RN-AG-04, RN-AG-05).
3. El paciente elige un horario y completa DNI, datos de contacto y consentimientos.
4. `POST /public/reservas`: en una transacción se busca o crea el paciente por DNI, se valida RN-AG-06 y se inserta el turno `RESERVADO` con `origen = ONLINE` y `token_publico`.
5. La exclusion constraint garantiza que no haya solapamiento aunque dos reservas lleguen al mismo tiempo.
6. Después del commit se encolan la notificación `CONFIRMACION_RESERVA` y la sincronización con Google, y se registra el evento de auditoría.
7. La SPA muestra la confirmación con el enlace del turno.

```
Paciente → SPA → API /public/reservas → Servicio Agenda → PostgreSQL
                                             │ commit
                                             ├─► Notificaciones (WhatsApp/email)
                                             └─► Google Calendar (async)
```

**Casos de error**:
- Violación de la exclusion constraint → HTTP 409, "ese horario se acaba de ocupar" y se recarga la disponibilidad.
- El paciente ya tiene 2 turnos online futuros → HTTP 422 con un mensaje que indica contactar al consultorio.
- Falla del envío de la confirmación → el turno queda creado; la notificación se reintenta (no se revierte la reserva).

## Flujo 2: Recordatorio y confirmación por WhatsApp

**Disparador**: job programado cada 5 minutos.
**Actor**: sistema.

**Pasos**:
1. El job selecciona los turnos `RESERVADO` con `inicio` dentro de las próximas `recordatorio_horas_antes` horas que no tienen una notificación `RECORDATORIO` (RN-NO-01, RN-NO-02).
2. Por cada uno inserta `notificacion` en estado `PENDIENTE`. El índice único evita duplicados (RN-NO-04).
3. Envía la plantilla `recordatorio_turno` por Cloud API con botones de respuesta rápida, guarda `proveedor_msg_id` y pasa a `ENVIADA`.
4. Meta envía webhooks de estado (`delivered`, `read`, `failed`) y la notificación se actualiza.
5. El paciente toca "Confirmo" o "Cancelar". Llega un webhook con el payload del botón, que incluye el ID del turno.
6. El servicio aplica la transición (RN-NO-06), responde al paciente con un mensaje de servicio dentro de la ventana de 24 h y audita el cambio.

```
Job → Notificaciones → Meta Cloud API → WhatsApp del paciente
                                             │ toca botón
Meta webhook → /webhooks/whatsapp → valida firma → Servicio Turnos → DB
```

**Casos de error**:
- Envío `failed` o paciente sin WhatsApp → respaldo por email (RN-NO-03).
- Webhook con firma inválida → HTTP 401 y se descarta.
- Webhook duplicado (Meta reintenta) → se procesa de forma idempotente por `message.id`.
- Respuesta "Cancelar" fuera de plazo → no se cancela; se avisa al consultorio y se responde con el teléfono.

## Flujo 3: Turnos sin confirmar

**Disparador**: job programado cada 15 minutos.
**Actor**: sistema → secretaria.

**Pasos**:
1. Selecciona los turnos `RESERVADO` con `inicio ≤ ahora + confirmacion_limite_horas` y `sin_confirmar_alertado = false`.
2. Marca `sin_confirmar_alertado = true` y los agrega a la bandeja "Sin confirmar".
3. Envía un resumen al consultorio (email; un aviso in-app en el panel).
4. La secretaria revisa la bandeja y decide: llamar o escribir (click-to-chat), cancelar, o dejarlo como está.
5. Si cancela, el hueco se muestra con la lista de espera sugerida (flujo 4).

**Casos de error**:
- El paciente confirma después de la alerta → el turno pasa a `CONFIRMADO` y sale de la bandeja.

## Flujo 4: Hueco liberado y oferta con un clic a la lista de espera

**Disparador**: un turno pasa a `CANCELADO`.
**Actor**: secretaria → pacientes de la lista de espera.

**Pasos**:
1. La agenda muestra el hueco y el panel sugiere las entradas `ACTIVA` de la lista de espera para esa prestación, de la más antigua a la más reciente.
2. La secretaria tiene dos caminos:
   - **Asignar directamente**: crea el turno (flujo de alta interna) y marca la entrada `ASIGNADA`.
   - **Ofrecer por WhatsApp**: selecciona entre 1 y 5 pacientes y confirma.
3. `POST /ofertas` crea la `oferta_hueco` `ABIERTA` con `vence_at` y un `oferta_destinatario` por paciente. La exclusion constraint impide una segunda oferta abierta sobre el mismo rango (RN-LE-07).
4. Después del commit, el outbox envía la plantilla `oferta_hueco` a cada destinatario, con el botón "Lo tomo" y un payload `OFER:<token_respuesta>`.
5. Un paciente toca "Lo tomo". Llega el webhook, se valida la firma y se resuelve el destinatario por token.
6. En una transacción: se verifica que la oferta siga `ABIERTA` y no vencida, se inserta el turno `CONFIRMADO` (la exclusion constraint de `turno` garantiza unicidad), se marcan la oferta `TOMADA`, el destinatario `ACEPTADA` y la entrada de lista de espera `ASIGNADA`, y se audita.
7. Se responde al ganador con la confirmación y el enlace del turno (ventana de 24 h abierta por su respuesta). El panel de la secretaria se actualiza.
8. Los que respondan después reciben "ese turno ya fue tomado" (destinatario `TARDE`) y siguen `ACTIVA`.
9. Un job cada minuto pasa a `VENCIDA` las ofertas con `vence_at` cumplido. El hueco vuelve a estar libre, incluso para la reserva online.

```
Secretaria → SPA → POST /ofertas → Servicio Lista de espera → DB (oferta ABIERTA)
                                         └─► Outbox → Meta Cloud API → WhatsApp de N pacientes
Paciente toca "Lo tomo" → Meta webhook → /webhooks/whatsapp → Servicio Ofertas
      → TX: turno CONFIRMADO + oferta TOMADA + entrada ASIGNADA → respuesta al paciente
```

**Casos de error**:
- Dos aceptaciones simultáneas → la segunda viola la exclusion constraint o encuentra la oferta `TOMADA`: rollback y respuesta "ya fue tomado".
- Alguien reservó el hueco por otra vía mientras la oferta estaba abierta (por ejemplo, la secretaria por teléfono) → la aceptación falla por la constraint; la oferta pasa a `CANCELADA` y se responde "ya fue tomado".
- Aceptación después de `vence_at` → "la oferta venció"; el paciente sigue en la lista.
- Envío fallido a un destinatario → se marca `FALLIDA` y se muestra en el panel; **no** hay respaldo por email para ofertas, porque el tiempo de respuesta es corto.

## Flujo 5: Atención y registro clínico

**Disparador**: el paciente llega al consultorio.
**Actor**: odontólogo.

**Pasos**:
1. El odontólogo abre el turno del día y entra a la ficha (auditoría `VER_HISTORIA`).
2. Revisa alergias y medicación destacadas y el odontograma vigente.
3. Registra la evolución y, si corresponde, una nueva versión del odontograma. Cada guardado es una entrada append-only con hash encadenado.
4. Marca el turno como `ATENDIDO`.
5. La secretaria registra el cobro.

**Casos de error**:
- Error de tipeo en una entrada → se crea una `ENMIENDA` que referencia la original; nunca se edita.

## Flujo 6: Autenticación interna

**Pasos**:
1. `POST /auth/login` con email y contraseña; se verifica el hash con BCrypt.
2. Si es válido, se crea una sesión con cookie `HttpOnly`, `Secure`, `SameSite=Strict`, más un token CSRF para la SPA.
3. Se audita `LOGIN` o `LOGIN_FALLIDO`; aplica el bloqueo de RN-SEG-03.
4. Las rutas se autorizan por rol con Spring Security.

## Flujo 7: Exportación y copia de historia clínica

**Pasos**:
1. El odontólogo pide la exportación completa (`POST /exportaciones`) o el PDF de un paciente.
2. Un job genera un ZIP con CSV/JSON por entidad y PDFs de historias clínicas, cifrado con una contraseña que elige el odontólogo.
3. Se audita `EXPORTAR`; el enlace de descarga vence a las 24 h.
