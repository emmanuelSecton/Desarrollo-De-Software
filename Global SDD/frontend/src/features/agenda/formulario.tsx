import type { Ref } from 'react'
import { ErrorDeApi } from '../../shared/api-client/cliente'
import {
  aHora,
  aMinutos,
  diaDeLaSemana,
  fechaDe,
  horaDe,
  minutosDelDia,
  type Fecha,
} from '../../shared/formatos/fechas'
import { Campo } from '../../shared/ui/Campo'
import { Selector } from '../../shared/ui/Selector'
import type { Franja, Turno } from './api'
import { esActivo, nombreDelPaciente } from './estado'
import estilos from './Formulario.module.css'

export const MAXIMA_DURACION = 480

export interface ErroresDeHorario {
  fecha?: string
  hora?: string
  duracion?: string
}

/** Duraciones validas: desde la de la prestacion hasta 8 horas, de a 5 minutos (RN-AG-02). */
export function opcionesDeDuracion(minima: number) {
  const opciones = []
  for (let minutos = minima; minutos <= MAXIMA_DURACION; minutos += 5) {
    opciones.push({ valor: String(minutos), texto: `${minutos} min` })
  }
  return opciones
}

export function validarHorario(fecha: string, hora: string): ErroresDeHorario {
  const errores: ErroresDeHorario = {}
  if (!fecha) errores.fecha = 'Elegí el día'
  if (!hora) errores.hora = 'Elegí la hora'
  else if (aMinutos(hora) % 5 !== 0) errores.hora = 'La hora tiene que ser múltiplo de 5 minutos'
  return errores
}

/** Mensajes por campo de la API ("errores"), con el nombre del campo del formulario. */
export function erroresPorCampo(error: unknown, nombres: Record<string, string> = {}) {
  if (!(error instanceof ErrorDeApi)) return {}
  return Object.fromEntries(error.errores.map((e) => [nombres[e.campo] ?? e.campo, e.mensaje]))
}

export const esProblema = (error: unknown, tipo: string): error is ErrorDeApi =>
  error instanceof ErrorDeApi && error.tipo === tipo

/** El horario cae fuera de las franjas de atencion de ese dia. */
export function fueraDeAtencion(franjas: Franja[], fecha: Fecha, hora: string, duracion: number) {
  if (!fecha || !hora) return false
  const inicio = aMinutos(hora)
  return !franjas.some(
    (f) =>
      f.diaSemana === diaDeLaSemana(fecha) &&
      inicio >= aMinutos(f.desde) &&
      inicio + duracion <= aMinutos(f.hasta),
  )
}

/** "10:00 a 10:30 · Jorge Pereyra (Control)": con que turnos cargados choca un horario. */
export function describirChoques(
  turnos: Turno[],
  fecha: Fecha,
  hora: string,
  duracion: number,
  zona: string,
  excepto?: string,
): string[] {
  const inicio = aMinutos(hora)
  const fin = inicio + duracion
  return turnos
    .filter(
      (t) => esActivo(t) && !t.sobreturno && t.id !== excepto && fechaDe(t.inicio, zona) === fecha,
    )
    .filter((t) => {
      const desde = minutosDelDia(t.inicio, zona)
      const hasta = desde + Math.round((Date.parse(t.fin) - Date.parse(t.inicio)) / 60_000)
      return desde < fin && inicio < hasta
    })
    .map(
      (t) =>
        `${horaDe(t.inicio, zona)} a ${horaDe(t.fin, zona)} · ${nombreDelPaciente(t)} (${t.prestacionNombre})`,
    )
}

interface PropsDeCamposDeHorario {
  fecha: string
  hora: string
  duracion: number
  duracionMinima: number
  errores: ErroresDeHorario
  refHora?: Ref<HTMLInputElement>
  alCambiar: (cambio: { fecha?: string; hora?: string; duracion?: number }) => void
}

/** Dia, hora (de a 5 minutos) y duracion, en la zona del consultorio. */
export function CamposDeHorario({
  fecha,
  hora,
  duracion,
  duracionMinima,
  errores,
  refHora,
  alCambiar,
}: PropsDeCamposDeHorario) {
  return (
    <div className={estilos.fila}>
      <Campo
        etiqueta="Día"
        type="date"
        value={fecha}
        error={errores.fecha}
        onChange={(e) => alCambiar({ fecha: e.target.value })}
      />
      <Campo
        ref={refHora}
        etiqueta="Hora"
        type="time"
        step={300}
        value={hora}
        error={errores.hora}
        onChange={(e) => alCambiar({ hora: e.target.value })}
      />
      <Selector
        etiqueta="Duración"
        opciones={opcionesDeDuracion(duracionMinima)}
        value={String(duracion)}
        error={errores.duracion}
        onChange={(e) => alCambiar({ duracion: Number(e.target.value) })}
      />
    </div>
  )
}

export const horaRedondeada = (minutos: number) => aHora(Math.ceil(minutos / 5) * 5)
