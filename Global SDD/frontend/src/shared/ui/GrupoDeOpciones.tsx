import { useId } from 'react'
import estilos from './Formularios.module.css'

interface PropsDeGrupoDeOpciones<T extends string> {
  leyenda: string
  opciones: { valor: T; texto: string }[]
  valor: T
  alCambiar: (valor: T) => void
}

/** Botones de opcion (radio) con aspecto de boton: objetivos tactiles de 44 px. */
export function GrupoDeOpciones<T extends string>({
  leyenda,
  opciones,
  valor,
  alCambiar,
}: PropsDeGrupoDeOpciones<T>) {
  const nombre = useId()
  return (
    <fieldset className={estilos.opciones}>
      <legend>{leyenda}</legend>
      {opciones.map((opcion) => (
        <label key={opcion.valor} className={estilos.opcion}>
          <input
            type="radio"
            name={nombre}
            value={opcion.valor}
            checked={valor === opcion.valor}
            onChange={() => alCambiar(opcion.valor)}
          />
          {opcion.texto}
        </label>
      ))}
    </fieldset>
  )
}
