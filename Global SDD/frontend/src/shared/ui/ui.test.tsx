import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { Alerta } from './Alerta'
import { AreaDeTexto } from './AreaDeTexto'
import { Boton } from './Boton'
import { Campo } from './Campo'
import { Dialogo } from './Dialogo'
import { EstadoVacio } from './EstadoVacio'
import { GrupoDeOpciones } from './GrupoDeOpciones'
import { Panel } from './Panel'
import { Selector } from './Selector'

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

  it('Selector asocia la etiqueta y marca el error', () => {
    render(
      <Selector
        etiqueta="Prestación"
        opciones={[{ valor: 'a', texto: 'Limpieza (45 min)' }]}
        error="Elegí la prestación"
      />,
    )

    const selector = screen.getByLabelText('Prestación')
    expect(selector).toHaveAttribute('aria-invalid', 'true')
    expect(selector).toHaveAccessibleDescription('Elegí la prestación')
  })

  it('AreaDeTexto cuenta los caracteres y no deja pasar del maximo', () => {
    render(<AreaDeTexto etiqueta="Motivo" value="Avisó" maximo={200} onChange={() => {}} />)

    const area = screen.getByLabelText('Motivo')
    expect(area).toHaveAttribute('maxlength', '200')
    expect(area).toHaveAccessibleDescription('5/200')
  })

  it('GrupoDeOpciones es un grupo de radios con leyenda', async () => {
    const alCambiar = vi.fn()
    render(
      <GrupoDeOpciones
        leyenda="¿Quién cancela?"
        opciones={[
          { valor: 'P', texto: 'El paciente' },
          { valor: 'C', texto: 'El consultorio' },
        ]}
        valor="P"
        alCambiar={alCambiar}
      />,
    )

    expect(screen.getByRole('group', { name: '¿Quién cancela?' })).toBeVisible()
    await userEvent.setup().click(screen.getByRole('radio', { name: 'El consultorio' }))
    expect(alCambiar).toHaveBeenCalledWith('C')
  })

  it('Panel abre modal con nombre, pone el foco inicial, cierra con la X y devuelve el foco', async () => {
    function Prueba() {
      const [abierto, setAbierto] = useState(false)
      return (
        <>
          <Boton onClick={() => setAbierto(true)}>Ver turno</Boton>
          <Panel
            abierto={abierto}
            titulo="Jorge Pereyra"
            alCerrar={() => setAbierto(false)}
            acciones={<Boton data-foco-inicial>Confirmar turno</Boton>}
          >
            <p>Detalle</p>
          </Panel>
        </>
      )
    }
    render(<Prueba />)
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: 'Ver turno' }))
    expect(screen.getByRole('dialog', { name: 'Jorge Pereyra' })).toHaveAttribute('open')
    expect(screen.getByRole('button', { name: 'Confirmar turno' })).toHaveFocus()

    await usuario.click(screen.getByRole('button', { name: 'Cerrar' }))
    expect(screen.getByRole('button', { name: 'Ver turno' })).toHaveFocus()
  })
})
