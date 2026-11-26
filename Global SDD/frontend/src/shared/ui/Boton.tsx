import type { ButtonHTMLAttributes } from 'react'
import estilos from './Boton.module.css'

export type VarianteDeBoton = 'primario' | 'secundario' | 'fantasma' | 'peligro'

/** Clases del boton, tambien para un enlace que se ve como boton. */
export function clasesDeBoton(variante: VarianteDeBoton = 'primario', bloque = false): string {
  return [estilos.boton, estilos[variante], bloque ? estilos.bloque : ''].filter(Boolean).join(' ')
}

interface PropsDeBoton extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: VarianteDeBoton
  bloque?: boolean
}

export function Boton({
  variante = 'primario',
  bloque = false,
  type = 'button',
  className,
  ...resto
}: PropsDeBoton) {
  const clases = [clasesDeBoton(variante, bloque), className].filter(Boolean).join(' ')
  return <button type={type} className={clases} {...resto} />
}
