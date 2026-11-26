import { X } from 'lucide-react'
import { useEffect, useId, useRef, type ReactNode } from 'react'
import estilos from './Panel.module.css'

interface PropsDePanel {
  abierto: boolean
  titulo: string
  /** Debajo del titulo (p. ej. la etiqueta de estado). */
  subtitulo?: ReactNode
  children: ReactNode
  acciones?: ReactNode
  alCerrar: () => void
}

/**
 * Panel lateral modal (hoja inferior en celular) sobre el <dialog> nativo: atrapa el foco,
 * cierra con Escape y devuelve el foco a quien lo abrio.
 */
export function Panel({ abierto, titulo, subtitulo, children, acciones, alCerrar }: PropsDePanel) {
  const ref = useRef<HTMLDialogElement>(null)
  const id = useId()

  useEffect(() => {
    const panel = ref.current
    if (!panel) return
    if (abierto && !panel.open) {
      const anterior = document.activeElement as HTMLElement | null
      panel.showModal()
      panel.querySelector<HTMLElement>('[data-foco-inicial]')?.focus()
      return () => anterior?.focus()
    }
    if (!abierto && panel.open) panel.close()
  }, [abierto])

  return (
    <dialog
      ref={ref}
      className={estilos.panel}
      aria-labelledby={`${id}-titulo`}
      onCancel={(evento) => {
        evento.preventDefault()
        alCerrar()
      }}
    >
      {abierto && (
        <>
          <div className={estilos.cabecera}>
            <div className={estilos.titulo}>
              <h2 id={`${id}-titulo`}>{titulo}</h2>
              {subtitulo}
            </div>
            <button type="button" className={estilos.cerrar} aria-label="Cerrar" onClick={alCerrar}>
              <X aria-hidden="true" />
            </button>
          </div>
          <div className={estilos.cuerpo}>{children}</div>
          {acciones && <div className={estilos.acciones}>{acciones}</div>}
        </>
      )}
    </dialog>
  )
}
