import logo from '@design-system/assets/logo.svg'
import estilos from './Marca.module.css'

/**
 * Nombre del producto (PA-11, definido el 2026-11-26). Tiene que coincidir con BRAND_NAME de
 * design-system/brand.js (lo controla un test); el logo sale de design-system/assets/logo.svg.
 */
export const NOMBRE_PRODUCTO = 'Odontogestión'

export function Marca() {
  return (
    <div className={estilos.marca}>
      <img className={estilos.logo} src={logo} alt="" />
      <span className={estilos.nombre}>{NOMBRE_PRODUCTO}</span>
    </div>
  )
}
