import type { Turno } from './api'

/** Estado que se muestra: el de la API, salvo RESERVADO cerca del turno, que se ve "Sin confirmar" (D9). */
export type EstadoVisible =
  'reservado' | 'sinconf' | 'confirmado' | 'cancelado' | 'atendido' | 'ausente'

export const TEXTO_DEL_ESTADO: Record<EstadoVisible, string> = {
  reservado: 'Reservado',
  sinconf: 'Sin confirmar',
  confirmado: 'Confirmado',
  cancelado: 'Cancelado',
  atendido: 'Atendido',
  ausente: 'Ausente',
}

const HORA_MS = 3_600_000

export function estadoVisible(
  turno: Pick<Turno, 'estado' | 'inicio'>,
  ahoraMs: number,
  limiteHoras: number,
): EstadoVisible {
  if (turno.estado === 'RESERVADO') {
    return Date.parse(turno.inicio) - ahoraMs <= limiteHoras * HORA_MS ? 'sinconf' : 'reservado'
  }
  return turno.estado.toLowerCase() as EstadoVisible
}

export const esActivo = (turno: Pick<Turno, 'estado'>) =>
  turno.estado === 'RESERVADO' || turno.estado === 'CONFIRMADO'

export const nombreDelPaciente = (turno: Pick<Turno, 'pacienteNombre' | 'pacienteApellido'>) =>
  `${turno.pacienteNombre} ${turno.pacienteApellido}`
