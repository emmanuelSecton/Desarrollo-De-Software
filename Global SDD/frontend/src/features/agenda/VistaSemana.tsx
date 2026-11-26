import {
  fechaDe,
  lunesDe,
  nombreCortoDelDia,
  numeroDelDia,
  sumarDias,
  type Fecha,
} from '../../shared/formatos/fechas'
import estilos from './Agenda.module.css'
import type { Turno } from './api'
import { estadoVisible } from './estado'
import { TarjetaDeTurno } from './TarjetaDeTurno'
import { franjasDelDia, ListaDelDia, type PropsDeVista } from './VistaDia'

interface PropsDeSemana extends PropsDeVista {
  esCelular: boolean
  /** Cambia la fecha elegida dentro de la semana (celular) o pasa a la vista dia (escritorio). */
  alElegirDia: (fecha: Fecha) => void
  alAbrirDia: (fecha: Fecha) => void
}

const cuenta = (n: number) => (n === 1 ? '1 turno' : `${n} turnos`)

export function VistaSemana(props: PropsDeSemana) {
  const { fecha, turnos, configuracion, ahoraMs, esCelular, alElegir, alElegirDia, alAbrirDia } =
    props
  const { zona, limiteConfirmacionHoras } = configuracion
  const hoy = fechaDe(ahoraMs, zona)
  const dias = Array.from({ length: 7 }, (_, i) => sumarDias(lunesDe(fecha), i))
  const turnosDe = (dia: Fecha): Turno[] => turnos.filter((t) => fechaDe(t.inicio, zona) === dia)

  if (esCelular) {
    return (
      <>
        <div className={estilos.tiraDias} role="group" aria-label="Día de la semana">
          {dias.map((dia) => (
            <button
              key={dia}
              type="button"
              aria-pressed={dia === fecha}
              aria-label={`${nombreCortoDelDia(dia)} ${numeroDelDia(dia)}, ${cuenta(turnosDe(dia).length)}`}
              onClick={() => alElegirDia(dia)}
            >
              {nombreCortoDelDia(dia)}
              <strong>{numeroDelDia(dia)}</strong>
              <span
                className={`${estilos.punto} ${turnosDe(dia).length === 0 ? estilos.puntoVacio : ''}`}
              />
            </button>
          ))}
        </div>
        <ListaDelDia {...props} turnos={turnosDe(fecha)} />
      </>
    )
  }

  return (
    <div className={estilos.semana}>
      {dias.map((dia) => {
        const delDia = turnosDe(dia)
        const sinAtencion = franjasDelDia(configuracion.franjas, dia).length === 0
        const clases = [
          estilos.diaCol,
          dia === hoy ? estilos.hoy : '',
          sinAtencion ? estilos.sinAtencion : '',
        ]
          .filter(Boolean)
          .join(' ')
        return (
          <section
            key={dia}
            className={clases}
            aria-label={`${nombreCortoDelDia(dia)} ${numeroDelDia(dia)}`}
            aria-current={dia === hoy ? 'date' : undefined}
          >
            <button
              type="button"
              className={estilos.cabeceraDia}
              aria-label={`Ver el ${nombreCortoDelDia(dia)} ${numeroDelDia(dia)}`}
              onClick={() => alAbrirDia(dia)}
            >
              <span>
                <span className={estilos.nombreDia}>{nombreCortoDelDia(dia)}</span>{' '}
                <span className={estilos.numero}>{numeroDelDia(dia)}</span>
              </span>
              {delDia.length > 0 && <span className={estilos.cuenta}>{cuenta(delDia.length)}</span>}
            </button>
            {delDia.length === 0 && (
              <p className={estilos.notaDia}>
                {sinAtencion ? 'Sin horario de atención' : 'Sin turnos'}
              </p>
            )}
            {delDia.map((turno) => (
              <TarjetaDeTurno
                key={turno.id}
                turno={turno}
                estado={estadoVisible(turno, ahoraMs, limiteConfirmacionHoras)}
                zona={zona}
                variante="compacta"
                alElegir={alElegir}
              />
            ))}
          </section>
        )
      })}
    </div>
  )
}
