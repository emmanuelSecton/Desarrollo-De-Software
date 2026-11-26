import { render } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router'
import { Rutas } from '../App'
import { ProveedorDeSesion } from '../shared/auth/sesion'

function UbicacionActual() {
  const { pathname, search } = useLocation()
  return <div data-testid="ubicacion">{pathname + search}</div>
}

/** La app completa en memoria, empezando en `ruta`. */
export function renderizarApp(ruta = '/') {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <ProveedorDeSesion>
        <Rutas />
        <UbicacionActual />
      </ProveedorDeSesion>
    </MemoryRouter>,
  )
}
