import { ChevronDown, CircleExclamationPoint } from 'lucide-react'
import { useId, type Ref, type SelectHTMLAttributes } from 'react'
import campo from './Campo.module.css'
import estilos from './Formularios.module.css'

interface Opcion {
  valor: string
  texto: string
}

interface PropsDeSelector extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> {
  etiqueta: string
  opciones: Opcion[]
  error?: string
  ref?: Ref<HTMLSelectElement>
}

export function Selector({ etiqueta, opciones, error, ref, ...resto }: PropsDeSelector) {
  const id = useId()
  return (
    <div className={campo.campo}>
      <label className={campo.etiqueta} htmlFor={id}>
        {etiqueta}
      </label>
      <div className={estilos.controlSelector}>
        <select
          ref={ref}
          id={id}
          className={estilos.selector}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${id}-error` : undefined}
          {...resto}
        >
          {opciones.map((opcion) => (
            <option key={opcion.valor} value={opcion.valor}>
              {opcion.texto}
            </option>
          ))}
        </select>
        <ChevronDown aria-hidden="true" />
      </div>
      {error && (
        <p className={campo.error} id={`${id}-error`} role="alert">
          <CircleExclamationPoint aria-hidden="true" />
          {error}
        </p>
      )}
    </div>
  )
}
