import { CalendarDays, Clock, Plus } from 'lucide-react'
import type { CSSProperties } from 'react'
import {
  diaDeLaSemana,
  fechaDe,
  formatearDia,
  horaLegible,
  minutosDelDia,
  type Fecha,
} from '../../shared/formatos/fechas'
import { Boton } from '../../shared/ui/Boton'
import { EstadoVacio } from '../../shared/ui/EstadoVacio'
import estilos from './Agenda.module.css'
import type { Franja, Turno } from './api'
import { disponerDia, enElDia, intervaloDeFranja, type Ubicacion } from './disposicion'
import { estadoVisible } from './estado'
import { TarjetaDeTurno } from './TarjetaDeTurno'
import type { ConfiguracionDeAgenda } from './useAgenda'

export interface PropsDeVista {
  fecha: Fecha
  turnos: Turno[]
  configuracion: ConfiguracionDeAgenda
  ahoraMs: number
  alElegir: (turno: Turno) => void
  alDarTurno: (fecha: Fecha, hora?: string) => void
}

export const franjasDelDia = (franjas: Franja[], fecha: Fecha) =>
  franjas
    .filter((f) => f.diaSemana === diaDeLaSemana(fecha))
    .sort((a, b) => a.desde.localeCompare(b.desde))

/** Posicion en la grilla: variables CSS asignadas desde JavaScript (compatible con la CSP, D4). */
const posicion = (ubicacion: Ubicacion, extra: Record<string, number> = {}) =>
  ({ '--fila': ubicacion.fila, '--filas': ubicacion.filas, ...extra }) as CSSProperties

/** Desde que minuto se ofrecen huecos: todo un dia futuro, lo que queda de hoy, nada de un dia pasado. */
function desdeMinuto(fecha: Fecha, zona: string, ahoraMs: number): number | null {
  const hoy = fechaDe(ahoraMs, zona)
  if (fecha < hoy) return null
  return fecha === hoy ? minutosDelDia(ahoraMs, zona) : 0
}

export function TextoDeFranjas({ franjas }: { franjas: Franja[] }) {
  if (franjas.length === 0) return <>Sin horario de atención</>
  const textos = franjas.map((f) => `${horaLegible(f.desde)} a ${horaLegible(f.hasta)}`)
  return <>Atención {textos.join(' y ')}</>
}

/** Vista dia en escritorio: grilla de 15 minutos con franjas, turnos y huecos libres. */
export function GrillaDelDia({
  fecha,
  turnos,
  configuracion,
  ahoraMs,
  alElegir,
  alDarTurno,
}: PropsDeVista) {
  const { zona, limiteConfirmacionHoras } = configuracion
  const franjas = franjasDelDia(configuracion.franjas, fecha)
  const disposicion = disponerDia(
    turnos.map((t) => enElDia(t, zona)),
    franjas.map(intervaloDeFranja),
    desdeMinuto(fecha, zona, ahoraMs),
  )
  if (!disposicion || turnos.length === 0) {
    return <DiaSinTurnos fecha={fecha} franjas={franjas} alDarTurno={alDarTurno} />
  }

  return (
    <div
      className={estilos.grilla}
      role="group"
      aria-label={`Turnos del ${formatearDia(fecha).toLowerCase()}`}
      style={{ '--total': disposicion.filas } as CSSProperties}
    >
      {disposicion.horas.map((hora) => (
        <div
          key={`h${hora.fila}`}
          className={`${estilos.horaCol} ${estilos.enGrilla}`}
          style={posicion(hora)}
        >
          {hora.texto}
        </div>
      ))}
      {disposicion.horas.map((hora) => (
        <div
          key={`l${hora.fila}`}
          className={`${estilos.linea} ${estilos.enGrilla}`}
          style={posicion(hora)}
        />
      ))}
      {disposicion.fuera.map((tramo) => (
        <div
          key={`f${tramo.fila}`}
          className={`${estilos.fuera} ${estilos.enGrilla}`}
          style={posicion(tramo)}
        >
          {tramo.texto}
        </div>
      ))}
      {disposicion.huecos.map((hueco) => (
        <button
          key={`v${hueco.fila}`}
          type="button"
          className={`${estilos.hueco} ${estilos.enGrilla}`}
          style={posicion(hueco)}
          onClick={() => alDarTurno(fecha, hueco.hora)}
        >
          <Plus aria-hidden="true" />
          Libre · Dar turno a las {horaLegible(hueco.hora)}
        </button>
      ))}
      {disposicion.grupos.map((grupo) => (
        <div
          key={grupo.turnos[0].id}
          className={`${estilos.carril} ${estilos.enGrilla}`}
          style={posicion(grupo, { '--carriles': grupo.turnos.length })}
        >
          {grupo.turnos.map((turno) => (
            <TarjetaDeTurno
              key={turno.id}
              turno={turno}
              estado={estadoVisible(turno, ahoraMs, limiteConfirmacionHoras)}
              zona={zona}
              variante="grilla"
              alElegir={alElegir}
            />
          ))}
        </div>
      ))}
    </div>
  )
}

/** Vista dia en celular (y dia elegido de la semana): lista agrupada por franja. */
export function ListaDelDia({
  fecha,
  turnos,
  configuracion,
  ahoraMs,
  alElegir,
  alDarTurno,
}: PropsDeVista) {
  const { zona, limiteConfirmacionHoras } = configuracion
  const franjas = franjasDelDia(configuracion.franjas, fecha)
  if (turnos.length === 0) {
    return <DiaSinTurnos fecha={fecha} franjas={franjas} alDarTurno={alDarTurno} />
  }

  const enFranja = (turno: Turno, franja: Franja) => {
    const hora = minutosDelDia(turno.inicio, zona)
    const { inicio, fin } = intervaloDeFranja(franja)
    return hora >= inicio && hora < fin
  }
  const grupos = franjas.map((franja) => ({
    titulo: `Atención ${horaLegible(franja.desde)} a ${horaLegible(franja.hasta)}`,
    turnos: turnos.filter((t) => enFranja(t, franja)),
  }))
  const fuera = turnos.filter((t) => !franjas.some((f) => enFranja(t, f)))
  if (fuera.length > 0) grupos.push({ titulo: 'Fuera del horario de atención', turnos: fuera })

  return (
    <div
      className={estilos.lista}
      role="group"
      aria-label={`Turnos del ${formatearDia(fecha).toLowerCase()}`}
    >
      {grupos
        .filter((grupo) => grupo.turnos.length > 0)
        .map((grupo) => (
          <div key={grupo.titulo} className={estilos.lista}>
            <p className={estilos.franjaTitulo}>
              <Clock aria-hidden="true" />
              {grupo.titulo}
            </p>
            {grupo.turnos.map((turno) => (
              <TarjetaDeTurno
                key={turno.id}
                turno={turno}
                estado={estadoVisible(turno, ahoraMs, limiteConfirmacionHoras)}
                zona={zona}
                variante="lista"
                alElegir={alElegir}
              />
            ))}
          </div>
        ))}
    </div>
  )
}

function DiaSinTurnos({
  fecha,
  franjas,
  alDarTurno,
}: {
  fecha: Fecha
  franjas: Franja[]
  alDarTurno: (fecha: Fecha) => void
}) {
  return (
    <>
      <EstadoVacio
        icono={<CalendarDays aria-hidden="true" />}
        titulo="No hay turnos para este día"
        texto={
          franjas.length === 0
            ? 'Este día no tiene horario de atención. Igual podés dar un turno.'
            : 'Todo el horario de atención está libre.'
        }
      />
      <Boton onClick={() => alDarTurno(fecha)}>
        <Plus aria-hidden="true" />
        Dar turno este día
      </Boton>
    </>
  )
}
