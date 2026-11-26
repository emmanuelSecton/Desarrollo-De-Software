import { CalendarClock, CircleCheck, CircleX } from 'lucide-react'
import { useEffect, useState } from 'react'
import { fechaDe, formatearDia, horaDe } from '../../shared/formatos/fechas'
import { Alerta } from '../../shared/ui/Alerta'
import { Boton } from '../../shared/ui/Boton'
import { Panel } from '../../shared/ui/Panel'
import { obtenerPaciente, type Paciente, type Turno } from './api'
import { estadoVisible, nombreDelPaciente } from './estado'
import estilos from './Formulario.module.css'
import { EtiquetaDeSobreturno, EtiquetaDeTurno } from './TarjetaDeTurno'
import type { ConfiguracionDeAgenda } from './useAgenda'

const ORIGEN: Record<string, string> = {
  INTERNO: 'Dado por el consultorio',
  ONLINE: 'Reserva online',
}

interface Props {
  turno: Turno
  configuracion: ConfiguracionDeAgenda
  ahoraMs: number
  confirmando: boolean
  alConfirmar: (turno: Turno) => void
  alReprogramar: (turno: Turno) => void
  alCancelar: (turno: Turno) => void
  alCerrar: () => void
}

/** Detalle del turno con las acciones que permite su estado (RN-TU-01). */
export function PanelDeTurno({
  turno,
  configuracion,
  ahoraMs,
  confirmando,
  alConfirmar,
  alReprogramar,
  alCancelar,
  alCerrar,
}: Props) {
  const { zona, limiteConfirmacionHoras } = configuracion
  const [paciente, setPaciente] = useState<Paciente | null>(null)
  const estado = estadoVisible(turno, ahoraMs, limiteConfirmacionHoras)
  const duracion = Math.round((Date.parse(turno.fin) - Date.parse(turno.inicio)) / 60_000)
  const activo = turno.estado === 'RESERVADO' || turno.estado === 'CONFIRMADO'

  useEffect(() => {
    let vigente = true
    obtenerPaciente(turno.pacienteId)
      .then((p) => vigente && setPaciente(p))
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [turno.pacienteId])

  return (
    <Panel
      abierto
      titulo={nombreDelPaciente(turno)}
      subtitulo={
        <span className={estilos.accionesAlerta}>
          <EtiquetaDeTurno estado={estado} />
          {turno.sobreturno && <EtiquetaDeSobreturno />}
        </span>
      }
      alCerrar={alCerrar}
      acciones={
        activo ? (
          <>
            {turno.estado === 'RESERVADO' && (
              <Boton data-foco-inicial disabled={confirmando} onClick={() => alConfirmar(turno)}>
                <CircleCheck aria-hidden="true" />
                {confirmando ? 'Confirmando…' : 'Confirmar turno'}
              </Boton>
            )}
            <Boton
              variante="secundario"
              data-foco-inicial={turno.estado === 'CONFIRMADO' || undefined}
              onClick={() => alReprogramar(turno)}
            >
              <CalendarClock aria-hidden="true" />
              Reprogramar
            </Boton>
            <Boton variante="secundario" onClick={() => alCancelar(turno)}>
              <CircleX aria-hidden="true" />
              Cancelar turno
            </Boton>
          </>
        ) : undefined
      }
    >
      {estado === 'sinconf' && (
        <Alerta tipo="aviso">
          Faltan menos de {limiteConfirmacionHoras} h y el paciente todavía no confirmó.
        </Alerta>
      )}
      {!activo && <p className={estilos.resumen}>Este turno ya no admite cambios.</p>}
      <dl className={estilos.datos}>
        <dt>Día</dt>
        <dd>{formatearDia(fechaDe(turno.inicio, zona))}</dd>
        <dt>Horario</dt>
        <dd>
          {horaDe(turno.inicio, zona)} a {horaDe(turno.fin, zona)} ({duracion} min)
        </dd>
        <dt>Prestación</dt>
        <dd>{turno.prestacionNombre}</dd>
        <dt>DNI</dt>
        <dd>{paciente?.dni ?? '…'}</dd>
        <dt>Teléfono</dt>
        <dd>{paciente?.telefono ?? '…'}</dd>
        <dt>Origen</dt>
        <dd>{ORIGEN[turno.origen] ?? turno.origen}</dd>
      </dl>
    </Panel>
  )
}
