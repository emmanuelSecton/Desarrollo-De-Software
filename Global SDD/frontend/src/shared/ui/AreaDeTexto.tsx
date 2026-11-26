import { CircleExclamationPoint } from 'lucide-react'
import { useId, type Ref, type TextareaHTMLAttributes } from 'react'
import campo from './Campo.module.css'
import estilos from './Formularios.module.css'

interface PropsDeAreaDeTexto extends Omit<
  TextareaHTMLAttributes<HTMLTextAreaElement>,
  'id' | 'value'
> {
  etiqueta: string
  value: string
  maximo: number
  error?: string
  ayuda?: string
  ref?: Ref<HTMLTextAreaElement>
}

/** Area de texto con contador de caracteres ("12/200"). */
export function AreaDeTexto({
  etiqueta,
  value,
  maximo,
  error,
  ayuda,
  ref,
  ...resto
}: PropsDeAreaDeTexto) {
  const id = useId()
  const descripcion = [error ? `${id}-error` : null, `${id}-contador`, ayuda ? `${id}-ayuda` : null]
    .filter(Boolean)
    .join(' ')
  return (
    <div className={campo.campo}>
      <label className={campo.etiqueta} htmlFor={id}>
        {etiqueta}
      </label>
      <textarea
        ref={ref}
        id={id}
        className={estilos.areaDeTexto}
        value={value}
        maxLength={maximo}
        aria-invalid={error ? true : undefined}
        aria-describedby={descripcion}
        {...resto}
      />
      {error && (
        <p className={campo.error} id={`${id}-error`} role="alert">
          <CircleExclamationPoint aria-hidden="true" />
          {error}
        </p>
      )}
      <span className={estilos.contador} id={`${id}-contador`}>
        {value.length}/{maximo}
      </span>
      {ayuda && (
        <p className={estilos.ayuda} id={`${id}-ayuda`}>
          {ayuda}
        </p>
      )}
    </div>
  )
}
