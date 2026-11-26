import estilos from './Cargando.module.css'

export function Cargando({ pantallaCompleta = false }: { pantallaCompleta?: boolean }) {
  const clase = pantallaCompleta
    ? `${estilos.cargando} ${estilos.pantallaCompleta}`
    : estilos.cargando
  return (
    <p className={clase} role="status">
      Cargando…
    </p>
  )
}
