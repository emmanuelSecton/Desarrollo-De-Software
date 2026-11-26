import { useSyncExternalStore } from 'react'

/** Mismo corte que los estilos: hasta 760 px la app usa la disposicion de celular. */
const CONSULTA = '(max-width: 760px)'

function suscribir(avisar: () => void) {
  if (typeof window.matchMedia !== 'function') return () => {}
  const consulta = window.matchMedia(CONSULTA)
  consulta.addEventListener('change', avisar)
  return () => consulta.removeEventListener('change', avisar)
}

const leer = () => typeof window.matchMedia === 'function' && window.matchMedia(CONSULTA).matches

/** Para elegir que se dibuja (grilla o lista), no solo como se ve. */
export function useEsCelular(): boolean {
  return useSyncExternalStore(suscribir, leer, () => false)
}
