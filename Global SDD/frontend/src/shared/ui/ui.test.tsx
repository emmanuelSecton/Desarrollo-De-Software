import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { Alerta } from './Alerta'
import { Boton } from './Boton'
import { Campo } from './Campo'
import { Dialogo } from './Dialogo'
import { EstadoVacio } from './EstadoVacio'

describe('componentes base', () => {
  it('Campo asocia la etiqueta y, con error, marca aria-invalid y lo anuncia', () => {
    render(<Campo etiqueta="Email" error="Ingresá tu email" />)

    const input = screen.getByLabelText('Email')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('Ingresá tu email')
    expect(screen.getByRole('alert')).toHaveTextContent('Ingresá tu email')
  })

  it('Campo sin error no lleva aria-invalid', () => {
    render(<Campo etiqueta="Email" />)

    expect(screen.getByLabelText('Email')).not.toHaveAttribute('aria-invalid')
  })

  it('Alerta de error usa role="alert" y las demas role="status", siempre con texto', () => {
    render(
      <>
        <Alerta tipo="error">Falló</Alerta>
        <Alerta tipo="exito">Listo</Alerta>
      </>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Falló')
    expect(screen.getByRole('status')).toHaveTextContent('Listo')
  })

  it('Boton es de tipo button salvo que se pida submit', () => {
    render(<Boton>Hacer</Boton>)

    expect(screen.getByRole('button', { name: 'Hacer' })).toHaveAttribute('type', 'button')
  })

  it('EstadoVacio es una seccion con nombre', () => {
    render(<EstadoVacio icono={null} titulo="Disponible próximamente" texto="Pronto." />)

    expect(screen.getByRole('region', { name: 'Disponible próximamente' })).toBeVisible()
  })

  it('Dialogo abre modal, pone el foco inicial y Escape llama a alCancelar', async () => {
    const alCancelar = vi.fn()
    function Prueba() {
      const [abierto, setAbierto] = useState(false)
      return (
        <>
          <Boton onClick={() => setAbierto(true)}>Abrir</Boton>
          <Dialogo
            abierto={abierto}
            titulo="Título del diálogo"
            rol="alertdialog"
            alCancelar={alCancelar}
            acciones={
              <>
                <Boton>Otra</Boton>
                <Boton data-foco-inicial>Principal</Boton>
              </>
            }
          >
            <p>Texto del diálogo</p>
          </Dialogo>
        </>
      )
    }
    render(<Prueba />)
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: 'Abrir' }))

    const dialogo = screen.getByRole('alertdialog', { name: 'Título del diálogo' })
    expect(dialogo).toHaveAttribute('open')
    expect(dialogo).toHaveAccessibleDescription('Texto del diálogo')
    expect(screen.getByRole('button', { name: 'Principal' })).toHaveFocus()

    dialogo.dispatchEvent(new Event('cancel', { cancelable: true }))
    expect(alCancelar).toHaveBeenCalledTimes(1)
  })
})
