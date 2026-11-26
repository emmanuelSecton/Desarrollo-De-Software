import {
  CalendarDays,
  CircleCheck,
  CircleX,
  Clock,
  Layers,
  UserCheck,
  UserX,
  type LucideIcon,
} from 'lucide-react'
import { horaDe } from '../../shared/formatos/fechas'
import type { Turno } from './api'
import { TEXTO_DEL_ESTADO, nombreDelPaciente, type EstadoVisible } from './estado'
import estilos from './Turno.module.css'

const ICONO_DEL_ESTADO: Record<EstadoVisible, LucideIcon> = {
  reservado: CalendarDays,
  sinconf: Clock,
  confirmado: CircleCheck,
  cancelado: CircleX,
  atendido: UserCheck,
  ausente: UserX,
}

/** Estado con color, icono y texto: nunca solo color. */
export function EtiquetaDeTurno({ estado }: { estado: EstadoVisible }) {
  const Icono = ICONO_DEL_ESTADO[estado]
  return (
    <span className={`${estilos.etiqueta} ${estilos[`e-${estado}`]}`}>
      <Icono aria-hidden="true" />
      {TEXTO_DEL_ESTADO[estado]}
    </span>
  )
}

export function EtiquetaDeSobreturno() {
  return (
    <span className={`${estilos.etiqueta} ${estilos['e-sobreturno']}`}>
      <Layers aria-hidden="true" />
      Sobreturno
    </span>
  )
}

interface PropsDeTarjeta {
  turno: Turno
  estado: EstadoVisible
  zona: string
  variante: 'grilla' | 'lista' | 'compacta'
  alElegir: (turno: Turno) => void
}

export function TarjetaDeTurno({ turno, estado, zona, variante, alElegir }: PropsDeTarjeta) {
  const desde = horaDe(turno.inicio, zona)
  const paciente = nombreDelPaciente(turno)
  const nombre = [
    desde,
    paciente,
    turno.prestacionNombre,
    TEXTO_DEL_ESTADO[estado],
    turno.sobreturno ? 'sobreturno' : null,
  ]
    .filter(Boolean)
    .join(', ')
  const clases = [
    estilos.turno,
    estilos[`t-${estado}`],
    turno.sobreturno ? estilos.sobreturno : '',
    variante === 'grilla' ? estilos.enGrilla : '',
    variante === 'compacta' ? estilos.compacta : '',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button type="button" className={clases} aria-label={nombre} onClick={() => alElegir(turno)}>
      <span className={estilos.hora}>
        {variante === 'compacta' ? desde : `${desde} – ${horaDe(turno.fin, zona)}`}
      </span>
      <span className={estilos.paciente}>{paciente}</span>
      <span className={estilos.prestacion}>{turno.prestacionNombre}</span>
      <span className={estilos.etiquetas}>
        <EtiquetaDeTurno estado={estado} />
        {turno.sobreturno && <EtiquetaDeSobreturno />}
      </span>
    </button>
  )
}
