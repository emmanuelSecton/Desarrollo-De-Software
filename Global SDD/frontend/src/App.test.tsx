import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App (placeholder tecnico)', () => {
  it('renderiza el texto de aplicacion en construccion dentro de un landmark main', () => {
    render(<App />)

    expect(screen.getByRole('main')).toHaveTextContent('Aplicación en construcción')
  })

  it('no define estilos propios en linea', () => {
    const { container } = render(<App />)

    expect(container.querySelectorAll('[style]')).toHaveLength(0)
  })
})
