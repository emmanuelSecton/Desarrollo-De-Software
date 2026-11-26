import logo from '@design-system/assets/logo.svg'
import estilos from './Marca.module.css'

/**
 * Nombre provisorio del producto (PA-11). Tiene que coincidir con BRAND_NAME de
 * design-system/brand.js (lo controla un test); el logo sale de design-system/assets/logo.svg.
 */
export const NOMBRE_PRODUCTO = 'Nombre del producto (TBD)'

export function Marca() {
  return (
    <div className={estilos.marca}>
      <img className={estilos.logo} src={logo} alt="" />
      <span className={estilos.nombre}>{NOMBRE_PRODUCTO}</span>
    </div>
  )
}

/** Indica que el nombre y el logo todavia no son definitivos. */
export function EtiquetaProvisoria({ texto = 'Provisorio' }: { texto?: string }) {
  return <span className={estilos.provisorio}>{texto}</span>
}
