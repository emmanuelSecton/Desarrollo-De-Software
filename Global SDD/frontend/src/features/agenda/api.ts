import { api } from '../../shared/api-client/cliente'
import type { Rango } from '../../shared/formatos/fechas'

// Llamadas de la agenda a la API (C-05, C-09, C-10). Las respuestas se usan tal cual: sin datos clinicos.

export type EstadoTurno = 'RESERVADO' | 'CONFIRMADO' | 'CANCELADO' | 'ATENDIDO' | 'AUSENTE'

export interface Turno {
  id: string
  inicio: string
  fin: string
  estado: EstadoTurno
  origen: string
  sobreturno: boolean
  version: number
  pacienteId: string
  pacienteNombre: string
  pacienteApellido: string
  prestacionId: string
  prestacionNombre: string
}

export interface Franja {
  id: string
  diaSemana: number
  desde: string
  hasta: string
}

export interface Prestacion {
  id: string
  nombre: string
  duracionMinutos: number
  activa: boolean
}

export interface Consultorio {
  confirmacionLimiteHoras: number
  zonaHoraria: string
}

export interface Paciente {
  id: string
  dni: string
  nombre: string
  apellido: string
  telefono: string
  email: string | null
  activo: boolean
}

export interface PaginaDePacientes {
  contenido: Paciente[]
  total: number
}

export interface AltaDePaciente {
  dni: string
  nombre: string
  apellido: string
  telefono: string
  email: string | null
  aceptaWhatsapp: boolean
  consentimientoDatos: boolean
}

export const PROBLEMAS_DE_TURNOS = {
  superpuesto: '/problemas/turno-superpuesto',
  modificado: '/problemas/turno-modificado',
  transicionInvalida: '/problemas/transicion-invalida',
  pacienteDuplicado: '/problemas/paciente-duplicado',
} as const

export const listarTurnos = ({ desde, hasta }: Rango) =>
  api<Turno[]>(`/turnos?desde=${encodeURIComponent(desde)}&hasta=${encodeURIComponent(hasta)}`)

export const listarFranjas = () => api<Franja[]>('/horarios')

export const listarPrestaciones = () => api<Prestacion[]>('/prestaciones')

export const obtenerConsultorio = () => api<Consultorio>('/consultorio')

export const buscarPacientes = (texto: string) =>
  api<PaginaDePacientes>(`/pacientes?q=${encodeURIComponent(texto)}&tamano=5`)

export const obtenerPaciente = (id: string) => api<Paciente>(`/pacientes/${id}`)

export const crearPaciente = (alta: AltaDePaciente) =>
  api<Paciente>('/pacientes', { metodo: 'POST', cuerpo: alta })

export interface AltaDeTurno {
  pacienteId: string
  prestacionId: string
  inicio: string
  duracionMinutos: number
  sobreturno: boolean
}

export const crearTurno = (alta: AltaDeTurno) =>
  api<{ id: string }>('/turnos', { metodo: 'POST', cuerpo: alta })

export const confirmarTurno = (turno: Pick<Turno, 'id' | 'version'>) =>
  api(`/turnos/${turno.id}/confirmar`, { metodo: 'POST', cuerpo: { version: turno.version } })

export const reprogramarTurno = (
  turno: Pick<Turno, 'id' | 'version'>,
  inicio: string,
  duracionMinutos: number,
) =>
  api(`/turnos/${turno.id}`, {
    metodo: 'PUT',
    cuerpo: { version: turno.version, inicio, duracionMinutos },
  })

export type CanceladoPor = 'PACIENTE' | 'CONSULTORIO'

export const cancelarTurno = (
  turno: Pick<Turno, 'id' | 'version'>,
  canceladoPor: CanceladoPor,
  motivo: string,
) =>
  api(`/turnos/${turno.id}/cancelar`, {
    metodo: 'POST',
    cuerpo: { version: turno.version, canceladoPor, motivo },
  })
