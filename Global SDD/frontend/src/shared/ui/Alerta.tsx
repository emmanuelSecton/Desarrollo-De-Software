import {
  CircleCheck,
  CircleExclamationPoint,
  Clock,
  Info,
  TriangleExclamationPoint,
  X,
} from 'lucide-react'
import type { ReactNode } from 'react'
import estilos from './Alerta.module.css'

export type TipoDeAlerta = 'error' | 'aviso' | 'info' | 'exito' | 'sesion'

const ICONOS = {
  error: CircleExclamationPoint,
  aviso: TriangleExclamationPoint,
  info: Info,
  exito: CircleCheck,
  sesion: Clock,
}

interface PropsDeAlerta {
  tipo: TipoDeAlerta
  titulo?: string
  id?: string
  alCerrar?: () => void
  children?: ReactNode
}

/**
 * Mensaje con icono y texto: el estado nunca se comunica solo con color.
 * Los errores se anuncian con role="alert"; el resto, con role="status".
 */
export function Alerta({ tipo, titulo, id, alCerrar, children }: PropsDeAlerta) {
  const Icono = ICONOS[tipo]
  const clase = tipo === 'sesion' ? estilos.info : estilos[tipo]
  return (
    <div
      id={id}
      className={`${estilos.alerta} ${clase}`}
      role={tipo === 'error' ? 'alert' : 'status'}
    >
      <Icono aria-hidden="true" />
      <span className={estilos.texto}>
        {titulo && <strong>{titulo}</strong>}
        {children}
      </span>
      {alCerrar && (
        <button
          type="button"
          className={estilos.cerrar}
          aria-label="Cerrar aviso"
          onClick={alCerrar}
        >
          <X aria-hidden="true" />
        </button>
      )}
    </div>
  )
}
