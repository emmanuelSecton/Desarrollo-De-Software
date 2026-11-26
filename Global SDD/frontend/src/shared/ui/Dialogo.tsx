import { useEffect, useId, useRef, type ReactNode } from 'react'
import estilos from './Dialogo.module.css'

interface PropsDeDialogo {
  abierto: boolean
  titulo: string
  children: ReactNode
  acciones: ReactNode
  /** Escape. */
  alCancelar: () => void
  /** alertdialog para avisos que piden una decision (p. ej. sesion por vencer). */
  rol?: 'dialog' | 'alertdialog'
}

/**
 * Dialogo modal sobre el <dialog> nativo: showModal() atrapa el foco y maneja Escape.
 * El foco inicial va al elemento marcado con data-foco-inicial.
 */
export function Dialogo({
  abierto,
  titulo,
  children,
  acciones,
  alCancelar,
  rol = 'dialog',
}: PropsDeDialogo) {
  const ref = useRef<HTMLDialogElement>(null)
  const id = useId()

  useEffect(() => {
    const dialogo = ref.current
    if (!dialogo) return
    if (abierto && !dialogo.open) {
      dialogo.showModal()
      dialogo.querySelector<HTMLElement>('[data-foco-inicial]')?.focus()
    } else if (!abierto && dialogo.open) {
      dialogo.close()
    }
  }, [abierto])

  return (
    <dialog
      ref={ref}
      className={estilos.dialogo}
      role={rol}
      aria-modal="true"
      aria-labelledby={`${id}-titulo`}
      aria-describedby={`${id}-texto`}
      onCancel={(evento) => {
        evento.preventDefault()
        alCancelar()
      }}
    >
      {abierto && (
        <>
          <h2 id={`${id}-titulo`}>{titulo}</h2>
          <div id={`${id}-texto`}>{children}</div>
          <div className={estilos.acciones}>{acciones}</div>
        </>
      )}
    </dialog>
  )
}
