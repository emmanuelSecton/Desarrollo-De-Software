import { SlidersHorizontal, Users } from 'lucide-react'
import { EstadoVacio } from '../shared/ui/EstadoVacio'

// Secciones de la app: muestran "Disponible próximamente" hasta que lleguen
// C-26 (pacientes) y C-08 (configuración). La agenda llegó con C-13 (features/agenda).

const PROXIMAMENTE = 'Disponible próximamente'

export function PacientesPage() {
  return (
    <>
      <h1>Pacientes</h1>
      <EstadoVacio
        icono={<Users aria-hidden="true" />}
        titulo={PROXIMAMENTE}
        texto="Acá vas a buscar pacientes por DNI, nombre o teléfono, y ver su ficha."
      />
    </>
  )
}

export function ConfiguracionPage() {
  return (
    <>
      <h1>Configuración</h1>
      <EstadoVacio
        icono={<SlidersHorizontal aria-hidden="true" />}
        titulo={PROXIMAMENTE}
        texto="Acá vas a configurar el consultorio, los horarios de atención, las prestaciones y los usuarios."
      />
    </>
  )
}
