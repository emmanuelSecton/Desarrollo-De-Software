import { ChevronLeft, ChevronRight, Clock, Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import {
  esFechaValida,
  formatearDia,
  formatearSemana,
  hoyEn,
  lunesDe,
  rangoDeDia,
  rangoDeSemana,
  sumarDias,
  type Fecha,
} from '../../shared/formatos/fechas'
import { Alerta } from '../../shared/ui/Alerta'
import { Boton } from '../../shared/ui/Boton'
import { Cargando } from '../../shared/ui/Cargando'
import { useEsCelular } from '../../shared/ui/useEsCelular'
import estilos from './Agenda.module.css'
import { confirmarTurno, PROBLEMAS_DE_TURNOS, type Turno } from './api'
import { DialogoDarTurno } from './DialogoDarTurno'
import { DialogoCancelar, DialogoReprogramar } from './DialogosDeTurno'
import { EtiquetaDeTurno } from './TarjetaDeTurno'
import { esProblema } from './formulario'
import { PanelDeTurno } from './PanelDeTurno'
import { useConfiguracion, useTurnos, type ConfiguracionDeAgenda } from './useAgenda'
import { franjasDelDia, GrillaDelDia, ListaDelDia, TextoDeFranjas } from './VistaDia'
import { VistaSemana } from './VistaSemana'

export const TEXTO_TURNO_MODIFICADO =
  'Otra persona modificó este turno. Actualizamos la agenda para que veas los cambios.'

type Vista = 'dia' | 'semana'

type DialogoAbierto =
  | { tipo: 'dar'; fecha: Fecha; hora?: string }
  | { tipo: 'reprogramar'; turno: Turno }
  | { tipo: 'cancelar'; turno: Turno }

interface Aviso {
  tipo: 'exito' | 'aviso' | 'error'
  texto: string
}

/** Seccion /agenda (C-13). `ahora` se inyecta en los tests. */
export function AgendaPage({ ahora = Date.now }: { ahora?: () => number }) {
  const { configuracion, error } = useConfiguracion()
  if (error) {
    return (
      <>
        <h1 className="sr-only">Agenda</h1>
        <Alerta tipo="error">
          No pudimos cargar la agenda. Recargá la página para intentar de nuevo.
        </Alerta>
      </>
    )
  }
  if (!configuracion) return <Cargando />
  return <Agenda configuracion={configuracion} ahora={ahora} />
}

const cuenta = (n: number) => (n === 1 ? '1 turno' : `${n} turnos`)

function Agenda({
  configuracion,
  ahora,
}: {
  configuracion: ConfiguracionDeAgenda
  ahora: () => number
}) {
  const { zona } = configuracion
  const [parametros, setParametros] = useSearchParams()
  const esCelular = useEsCelular()
  const ahoraMs = ahora()
  const hoy = hoyEn(zona, ahoraMs)
  const vista: Vista = parametros.get('vista') === 'semana' ? 'semana' : 'dia'
  const parametroFecha = parametros.get('fecha')
  const fecha = esFechaValida(parametroFecha) ? parametroFecha : hoy

  const rango = useMemo(
    () => (vista === 'dia' ? rangoDeDia(fecha, zona) : rangoDeSemana(fecha, zona)),
    [vista, fecha, zona],
  )
  const { turnos, cargando, error, recargar } = useTurnos(rango)
  const [mostrarCancelados, setMostrarCancelados] = useState(false)
  const [seleccionado, setSeleccionado] = useState<Turno | null>(null)
  const [dialogo, setDialogo] = useState<DialogoAbierto | null>(null)
  const [aviso, setAviso] = useState<Aviso | null>(null)
  const [confirmando, setConfirmando] = useState(false)

  const visibles = mostrarCancelados ? turnos : turnos.filter((t) => t.estado !== 'CANCELADO')
  const activos = turnos.filter((t) => t.estado !== 'CANCELADO').length

  function ir(cambios: { vista?: Vista; fecha?: Fecha }) {
    setAviso(null)
    setParametros((actuales) => {
      const nuevos = new URLSearchParams(actuales)
      if (cambios.vista) nuevos.set('vista', cambios.vista)
      if (cambios.fecha) nuevos.set('fecha', cambios.fecha)
      return nuevos
    })
  }

  const paso = vista === 'dia' ? 1 : 7

  function terminar(fechaDelTurno: Fecha, mensaje: string) {
    setDialogo(null)
    setSeleccionado(null)
    const visible =
      vista === 'dia' ? fechaDelTurno === fecha : lunesDe(fechaDelTurno) === lunesDe(fecha)
    if (!visible) ir({ fecha: fechaDelTurno })
    setAviso({ tipo: 'exito', texto: mensaje })
    recargar()
  }

  function conflicto() {
    setDialogo(null)
    setSeleccionado(null)
    setAviso({ tipo: 'aviso', texto: TEXTO_TURNO_MODIFICADO })
    recargar()
  }

  async function confirmar(turno: Turno) {
    setConfirmando(true)
    try {
      await confirmarTurno(turno)
      terminar(fecha, 'Turno confirmado')
    } catch (problema) {
      if (
        esProblema(problema, PROBLEMAS_DE_TURNOS.modificado) ||
        esProblema(problema, PROBLEMAS_DE_TURNOS.transicionInvalida)
      ) {
        conflicto()
      } else {
        setAviso({ tipo: 'error', texto: 'No pudimos confirmar el turno. Probá de nuevo.' })
      }
    } finally {
      setConfirmando(false)
    }
  }

  const propsDeVista = {
    fecha,
    turnos: visibles,
    configuracion,
    ahoraMs,
    alElegir: setSeleccionado,
    alDarTurno: (dia: Fecha, hora?: string) => setDialogo({ tipo: 'dar', fecha: dia, hora }),
  }

  const titulo = vista === 'dia' ? formatearDia(fecha) : formatearSemana(fecha)
  const contieneHoy = vista === 'dia' ? fecha === hoy : lunesDe(fecha) === lunesDe(hoy)
  const subtitulo = [
    contieneHoy ? (vista === 'dia' ? 'Hoy' : 'Semana actual') : null,
    cuenta(activos),
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <div className={estilos.agenda}>
      <h1 className="sr-only">Agenda</h1>
      <div className={estilos.barra}>
        <div className={estilos.fecha}>
          <h2>{titulo}</h2>
          <span className={estilos.sub}>{cargando ? 'Cargando…' : subtitulo}</span>
        </div>
        <span className={estilos.espacio} />
        <div className={estilos.navegacion}>
          <Boton
            variante="secundario"
            className={estilos.botonIcono}
            aria-label={vista === 'dia' ? 'Día anterior' : 'Semana anterior'}
            onClick={() => ir({ fecha: sumarDias(fecha, -paso) })}
          >
            <ChevronLeft aria-hidden="true" />
          </Boton>
          <Boton variante="secundario" onClick={() => ir({ fecha: hoy })}>
            Hoy
          </Boton>
          <Boton
            variante="secundario"
            className={estilos.botonIcono}
            aria-label={vista === 'dia' ? 'Día siguiente' : 'Semana siguiente'}
            onClick={() => ir({ fecha: sumarDias(fecha, paso) })}
          >
            <ChevronRight aria-hidden="true" />
          </Boton>
        </div>
        <div className={estilos.segmentado} role="group" aria-label="Vista">
          <button type="button" aria-pressed={vista === 'dia'} onClick={() => ir({ vista: 'dia' })}>
            Día
          </button>
          <button
            type="button"
            aria-pressed={vista === 'semana'}
            onClick={() => ir({ vista: 'semana' })}
          >
            Semana
          </button>
        </div>
        <label className={estilos.casilla}>
          <input
            type="checkbox"
            checked={mostrarCancelados}
            onChange={(e) => setMostrarCancelados(e.target.checked)}
          />
          <span>
            Cancelados<span className={estilos.largo}> visibles</span>
          </span>
        </label>
        <Boton className={estilos.darTurno} onClick={() => setDialogo({ tipo: 'dar', fecha })}>
          <Plus aria-hidden="true" />
          Dar turno
        </Boton>
      </div>

      {vista === 'dia' && (
        <p className={estilos.leyenda}>
          <span>
            <Clock aria-hidden="true" />
            <TextoDeFranjas franjas={franjasDelDia(configuracion.franjas, fecha)} />
          </span>
          <span>
            <EtiquetaDeTurno estado="sinconf" /> faltan menos de{' '}
            {configuracion.limiteConfirmacionHoras} h y no confirmó
          </span>
        </p>
      )}

      {aviso && (
        <div className={estilos.aviso}>
          <Alerta tipo={aviso.tipo} alCerrar={() => setAviso(null)}>
            {aviso.texto}
          </Alerta>
        </div>
      )}

      {error ? (
        <Alerta tipo="error">
          No pudimos cargar los turnos. Recargá la página para intentar de nuevo.
        </Alerta>
      ) : cargando ? (
        <Cargando />
      ) : vista === 'semana' ? (
        <VistaSemana
          {...propsDeVista}
          esCelular={esCelular}
          alElegirDia={(dia) => ir({ fecha: dia })}
          alAbrirDia={(dia) => ir({ vista: 'dia', fecha: dia })}
        />
      ) : esCelular ? (
        <ListaDelDia {...propsDeVista} />
      ) : (
        <GrillaDelDia {...propsDeVista} />
      )}

      {seleccionado && !dialogo && (
        <PanelDeTurno
          turno={seleccionado}
          configuracion={configuracion}
          ahoraMs={ahoraMs}
          confirmando={confirmando}
          alConfirmar={(turno) => void confirmar(turno)}
          alReprogramar={(turno) => setDialogo({ tipo: 'reprogramar', turno })}
          alCancelar={(turno) => setDialogo({ tipo: 'cancelar', turno })}
          alCerrar={() => setSeleccionado(null)}
        />
      )}
      {dialogo?.tipo === 'dar' && (
        <DialogoDarTurno
          fecha={dialogo.fecha}
          hora={dialogo.hora}
          configuracion={configuracion}
          turnos={turnos}
          alListo={terminar}
          alCerrar={() => setDialogo(null)}
        />
      )}
      {dialogo?.tipo === 'reprogramar' && (
        <DialogoReprogramar
          turno={dialogo.turno}
          configuracion={configuracion}
          turnos={turnos}
          alListo={terminar}
          alConflicto={conflicto}
          alCerrar={() => setDialogo(null)}
        />
      )}
      {dialogo?.tipo === 'cancelar' && (
        <DialogoCancelar
          turno={dialogo.turno}
          configuracion={configuracion}
          alListo={terminar}
          alConflicto={conflicto}
          alCerrar={() => setDialogo(null)}
        />
      )}
    </div>
  )
}
