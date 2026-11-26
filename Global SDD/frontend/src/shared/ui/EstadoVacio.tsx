import { useId, type ReactNode } from 'react'
import estilos from './EstadoVacio.module.css'

/** Circulo con el icono de una seccion; tambien lo usan las paginas de error. */
export function IconoGrande({ children }: { children: ReactNode }) {
  return <span className={estilos.iconoGrande}>{children}</span>
}

interface PropsDeEstadoVacio {
  icono: ReactNode
  titulo: string
  texto: string
}

export function EstadoVacio({ icono, titulo, texto }: PropsDeEstadoVacio) {
  const id = useId()
  return (
    <section className={estilos.vacio} aria-labelledby={id}>
      <IconoGrande>{icono}</IconoGrande>
      <h2 id={id}>{titulo}</h2>
      <p className={estilos.texto}>{texto}</p>
    </section>
  )
}
