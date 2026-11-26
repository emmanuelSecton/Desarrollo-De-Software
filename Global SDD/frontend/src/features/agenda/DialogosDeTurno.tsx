import { CalendarClock } from 'lucide-react'
import { useRef, useState, type FormEvent } from 'react'
import { ErrorDeApi } from '../../shared/api-client/cliente'
import {
  fechaDe,
  formatearDiaCorto,
  horaDe,
  instanteDe,
  type Fecha,
} from '../../shared/formatos/fechas'
import { Alerta } from '../../shared/ui/Alerta'
import { AreaDeTexto } from '../../shared/ui/AreaDeTexto'
import { Boton } from '../../shared/ui/Boton'
import { Dialogo } from '../../shared/ui/Dialogo'
import { GrupoDeOpciones } from '../../shared/ui/GrupoDeOpciones'
import {
  cancelarTurno,
  PROBLEMAS_DE_TURNOS,
  reprogramarTurno,
  type CanceladoPor,
  type Turno,
} from './api'
import { nombreDelPaciente } from './estado'
import {
  CamposDeHorario,
  describirChoques,
  erroresPorCampo,
  esProblema,
  validarHorario,
  type ErroresDeHorario,
} from './formulario'
import estilos from './Formulario.module.css'
import type { ConfiguracionDeAgenda } from './useAgenda'

const ERROR_SIN_CONEXION =
  'No pudimos conectar con el consultorio. Probá de nuevo en unos segundos.'
export const MAXIMO_MOTIVO = 200

const esConflictoDeVersion = (error: unknown) =>
  esProblema(error, PROBLEMAS_DE_TURNOS.modificado) ||
  esProblema(error, PROBLEMAS_DE_TURNOS.transicionInvalida)

function Resumen({ turno, zona }: { turno: Turno; zona: string }) {
  return (
    <p className={estilos.resumen}>
      {nombreDelPaciente(turno)} · {turno.prestacionNombre} ·{' '}
      {formatearDiaCorto(fechaDe(turno.inicio, zona))} de {horaDe(turno.inicio, zona)} a{' '}
      {horaDe(turno.fin, zona)}
    </p>
  )
}

interface PropsComunes {
  turno: Turno
  configuracion: ConfiguracionDeAgenda
  alListo: (fecha: Fecha, mensaje: string) => void
  /** 409 por version vieja o transicion invalida: la agenda avisa y recarga. */
  alConflicto: () => void
  alCerrar: () => void
}

/** Reprogramar: el turno actual queda cancelado como "Reprogramado" y se crea uno nuevo (RN-TU-03). */
export function DialogoReprogramar({
  turno,
  configuracion,
  turnos,
  alListo,
  alConflicto,
  alCerrar,
}: PropsComunes & { turnos: Turno[] }) {
  const { zona } = configuracion
  const duracionActual = Math.round((Date.parse(turno.fin) - Date.parse(turno.inicio)) / 60_000)
  const [fecha, setFecha] = useState(fechaDe(turno.inicio, zona))
  const [hora, setHora] = useState(horaDe(turno.inicio, zona))
  const [duracion, setDuracion] = useState(duracionActual)
  const [errores, setErrores] = useState<ErroresDeHorario>({})
  const [choques, setChoques] = useState<string[] | null>(null)
  const [falla, setFalla] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const refHora = useRef<HTMLInputElement>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const nuevos = validarHorario(fecha, hora)
    setErrores(nuevos)
    setFalla(null)
    if (Object.keys(nuevos).length > 0) return
    setEnviando(true)
    try {
      await reprogramarTurno(turno, instanteDe(fecha, hora, zona), duracion)
      alListo(fecha, 'Turno reprogramado')
    } catch (error) {
      setEnviando(false)
      if (esConflictoDeVersion(error)) return alConflicto()
      if (esProblema(error, PROBLEMAS_DE_TURNOS.superpuesto)) {
        setChoques(describirChoques(turnos, fecha, hora, duracion, zona, turno.id))
        return
      }
      if (error instanceof ErrorDeApi && error.errores.length > 0) {
        setErrores(erroresPorCampo(error, { inicio: 'hora', duracionMinutos: 'duracion' }))
        return
      }
      setFalla(error instanceof ErrorDeApi ? error.detalle || error.titulo : ERROR_SIN_CONEXION)
    }
  }

  return (
    <Dialogo
      abierto
      ancho
      conCerrar
      describir={false}
      titulo="Reprogramar turno"
      alCancelar={alCerrar}
      acciones={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Volver
          </Boton>
          <Boton type="submit" form="form-reprogramar" disabled={enviando}>
            <CalendarClock aria-hidden="true" />
            {enviando ? 'Reprogramando…' : 'Reprogramar'}
          </Boton>
        </>
      }
    >
      <Resumen turno={turno} zona={zona} />
      <form id="form-reprogramar" noValidate onSubmit={enviar}>
        <CamposDeHorario
          fecha={fecha}
          hora={hora}
          duracion={duracion}
          duracionMinima={
            configuracion.prestaciones.find((p) => p.id === turno.prestacionId)?.duracionMinutos ??
            5
          }
          errores={errores}
          refHora={refHora}
          alCambiar={(cambio) => {
            if (cambio.fecha !== undefined) setFecha(cambio.fecha)
            if (cambio.hora !== undefined) setHora(cambio.hora)
            if (cambio.duracion !== undefined) setDuracion(cambio.duracion)
            setChoques(null)
          }}
        />
      </form>
      {choques ? (
        <Alerta tipo="error" titulo="Ese horario se superpone con otro turno">
          {choques.length > 0 ? `${choques.join('; ')}. ` : ''}Elegí otro horario: al reprogramar no
          se puede dar como sobreturno.
          <span className={estilos.accionesAlerta}>
            <Boton
              variante="secundario"
              onClick={() => {
                setChoques(null)
                refHora.current?.focus()
              }}
            >
              Elegir otro horario
            </Boton>
          </span>
        </Alerta>
      ) : (
        <Alerta tipo="info">
          El turno actual queda cancelado con el motivo “Reprogramado” y se crea uno nuevo en el
          horario elegido.
        </Alerta>
      )}
      {falla && <Alerta tipo="error">{falla}</Alerta>}
    </Dialogo>
  )
}

/** Cancelar con quien cancela y motivo obligatorio; el horario queda libre. */
export function DialogoCancelar({
  turno,
  configuracion,
  alListo,
  alConflicto,
  alCerrar,
}: PropsComunes) {
  const [canceladoPor, setCanceladoPor] = useState<CanceladoPor>('PACIENTE')
  const [motivo, setMotivo] = useState('')
  const [error, setError] = useState<string | undefined>()
  const [falla, setFalla] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const refMotivo = useRef<HTMLTextAreaElement>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setFalla(null)
    if (!motivo.trim()) {
      setError('Escribí el motivo de la cancelación')
      refMotivo.current?.focus()
      return
    }
    setError(undefined)
    setEnviando(true)
    try {
      await cancelarTurno(turno, canceladoPor, motivo.trim())
      alListo(fechaDe(turno.inicio, configuracion.zona), 'Turno cancelado')
    } catch (problema) {
      setEnviando(false)
      if (esConflictoDeVersion(problema)) return alConflicto()
      const porCampo = erroresPorCampo(problema)
      if (porCampo.motivo) return setError(porCampo.motivo)
      setFalla(
        problema instanceof ErrorDeApi ? problema.detalle || problema.titulo : ERROR_SIN_CONEXION,
      )
    }
  }

  return (
    <Dialogo
      abierto
      ancho
      conCerrar
      describir={false}
      titulo="Cancelar turno"
      alCancelar={alCerrar}
      acciones={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Volver
          </Boton>
          <Boton type="submit" form="form-cancelar" variante="peligro" disabled={enviando}>
            {enviando ? 'Cancelando…' : 'Cancelar turno'}
          </Boton>
        </>
      }
    >
      <Resumen turno={turno} zona={configuracion.zona} />
      <form id="form-cancelar" className={estilos.seccionSinBorde} noValidate onSubmit={enviar}>
        <GrupoDeOpciones<CanceladoPor>
          leyenda="¿Quién cancela?"
          opciones={[
            { valor: 'PACIENTE', texto: 'El paciente' },
            { valor: 'CONSULTORIO', texto: 'El consultorio' },
          ]}
          valor={canceladoPor}
          alCambiar={setCanceladoPor}
        />
        <AreaDeTexto
          ref={refMotivo}
          etiqueta="Motivo"
          value={motivo}
          maximo={MAXIMO_MOTIVO}
          error={error}
          ayuda="El horario queda libre para otro turno. No escribas datos de salud en el motivo."
          data-foco-inicial
          onChange={(e) => setMotivo(e.target.value)}
        />
      </form>
      {falla && <Alerta tipo="error">{falla}</Alerta>}
    </Dialogo>
  )
}
