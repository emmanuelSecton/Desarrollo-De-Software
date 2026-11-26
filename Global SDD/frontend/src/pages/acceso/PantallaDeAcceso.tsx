import type { ReactNode } from 'react'
import { Marca } from '../../shared/marca/Marca'
import estilos from './Acceso.module.css'

interface PropsDePantallaDeAcceso {
  titulo: string
  bajada: string
  children: ReactNode
  pie?: ReactNode
}

/** Tarjeta centrada del login y del cambio de contraseña obligatorio. */
export function PantallaDeAcceso({ titulo, bajada, children, pie }: PropsDePantallaDeAcceso) {
  return (
    <main className={estilos.acceso}>
      <section className={estilos.tarjeta} aria-labelledby="titulo-acceso">
        <div className={estilos.encabezado}>
          <Marca />
          <h1 id="titulo-acceso">{titulo}</h1>
          <p className={estilos.bajada}>{bajada}</p>
        </div>
        {children}
        {pie && <div className={estilos.pie}>{pie}</div>}
      </section>
    </main>
  )
}

export const claseDeFormulario = estilos.formulario
