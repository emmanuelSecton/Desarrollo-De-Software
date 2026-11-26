import { CalendarDays, SlidersHorizontal, Users } from 'lucide-react'
import { EstadoVacio } from '../shared/ui/EstadoVacio'

// Secciones de la app: muestran "Disponible próximamente" hasta que lleguen
// C-13 (agenda), C-26 (pacientes) y C-08 (configuración).

const PROXIMAMENTE = 'Disponible próximamente'

export function AgendaPage() {
  return (
    <>
      <h1>Agenda</h1>
      <EstadoVacio
        icono={<CalendarDays aria-hidden="true" />}
        titulo={PROXIMAMENTE}
        texto="Acá vas a ver los turnos del día y de la semana, y dar turnos nuevos."
      />
    </>
  )
}

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
