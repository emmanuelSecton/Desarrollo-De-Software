import { CircleExclamationPoint, Eye, EyeOff } from 'lucide-react'
import { useId, useState, type InputHTMLAttributes, type ReactNode, type Ref } from 'react'
import estilos from './Campo.module.css'

interface PropsDeCampo extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  etiqueta: string
  /** Mensaje de error del campo: lo marca como invalido y se anuncia con role="alert". */
  error?: string
  /** Marca el campo como invalido aunque el mensaje este en otro lado (p. ej. la alerta del login). */
  invalido?: boolean
  /** Ids de otros elementos que describen el campo (requisitos, alerta). */
  descritoPor?: string
  /** Para contraseñas: agrega el boton de mostrar u ocultar. */
  revelable?: boolean
  ref?: Ref<HTMLInputElement>
  children?: ReactNode
}

export function Campo({
  etiqueta,
  error,
  invalido = false,
  descritoPor,
  revelable = false,
  type = 'text',
  ref,
  children,
  ...resto
}: PropsDeCampo) {
  const id = useId()
  const idError = `${id}-error`
  const [visible, setVisible] = useState(false)
  const descripcion = [error ? idError : null, descritoPor].filter(Boolean).join(' ') || undefined

  return (
    <div className={estilos.campo}>
      <label className={estilos.etiqueta} htmlFor={id}>
        {etiqueta}
      </label>
      <div className={estilos.control}>
        <input
          ref={ref}
          id={id}
          type={revelable && visible ? 'text' : type}
          className={[estilos.input, revelable ? estilos.conAccion : ''].filter(Boolean).join(' ')}
          aria-invalid={error || invalido ? true : undefined}
          aria-describedby={descripcion}
          {...resto}
        />
        {revelable && (
          <button
            type="button"
            className={estilos.accion}
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={visible}
            onClick={() => setVisible((v) => !v)}
          >
            {visible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
          </button>
        )}
      </div>
      {error && (
        <p className={estilos.error} id={idError} role="alert">
          <CircleExclamationPoint aria-hidden="true" />
          {error}
        </p>
      )}
      {children}
    </div>
  )
}
