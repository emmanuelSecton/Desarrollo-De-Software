// Datos ficticios para la demo (SU-07): nombres, DNI y telefonos inventados.
// Este modulo solo calcula que cargar; no llama a la API.

/** Pacientes ficticios. Telefonos 11 5555 01xx: numeros de prueba, no de personas reales. */
export const PACIENTES = [
  { dni: '30111222', nombre: 'Lucía', apellido: 'Martínez', telefono: '1155550101' },
  { dni: '33777888', nombre: 'Jorge', apellido: 'Pereyra', telefono: '1155550122' },
  { dni: '35222333', nombre: 'Ana', apellido: 'Gómez', telefono: '1155550133' },
  { dni: '28999000', nombre: 'Carla', apellido: 'Ruiz', telefono: '1155550144' },
  { dni: '40123456', nombre: 'Diego', apellido: 'Fernández', telefono: '1155550155' },
  { dni: '31456789', nombre: 'Sofía', apellido: 'Herrera', telefono: '1155550166' },
  { dni: '29888777', nombre: 'Martín', apellido: 'Sosa', telefono: '1155550177' },
  { dni: '38555444', nombre: 'Inés', apellido: 'Vega', telefono: '1155550188' },
  { dni: '36444555', nombre: 'Tomás', apellido: 'Acosta', telefono: '1155550199' },
  { dni: '34666777', nombre: 'Paula', apellido: 'Díaz', telefono: '1155550110' },
  { dni: '27333444', nombre: 'Valeria', apellido: 'Romero', telefono: '1155550111' },
  { dni: '39222111', nombre: 'Lucas', apellido: 'Benítez', telefono: '1155550112' },
]

/**
 * Turnos por dia habil relativo (0 = el primer dia del plan si atiende, si no el proximo con atencion).
 * accion: 'confirmar', 'cancelar' o ninguna; sobreturno: se superpone a proposito con otro turno.
 */
export const TURNOS = [
  { dia: 0, hora: '09:00', paciente: 0, prestacion: 'Consulta/diagnóstico', accion: 'confirmar' },
  { dia: 0, hora: '10:00', paciente: 1, prestacion: 'Control' },
  { dia: 0, hora: '10:05', paciente: 2, prestacion: 'Control', sobreturno: true },
  { dia: 0, hora: '11:00', paciente: 3, prestacion: 'Endodoncia', accion: 'confirmar' },
  { dia: 0, hora: '15:30', paciente: 4, prestacion: 'Limpieza' },
  { dia: 0, hora: '17:00', paciente: 5, prestacion: 'Obturación', accion: 'confirmar' },
  { dia: 1, hora: '09:30', paciente: 6, prestacion: 'Limpieza', accion: 'confirmar' },
  { dia: 1, hora: '11:00', paciente: 7, prestacion: 'Extracción' },
  { dia: 1, hora: '12:00', paciente: 8, prestacion: 'Control', accion: 'cancelar' },
  { dia: 1, hora: '16:00', paciente: 9, prestacion: 'Consulta/diagnóstico' },
  { dia: 2, hora: '09:00', paciente: 10, prestacion: 'Endodoncia', accion: 'confirmar' },
  { dia: 2, hora: '15:00', paciente: 11, prestacion: 'Limpieza' },
  { dia: 2, hora: '16:30', paciente: 0, prestacion: 'Control' },
  { dia: 3, hora: '10:00', paciente: 1, prestacion: 'Obturación' },
  { dia: 3, hora: '11:30', paciente: 4, prestacion: 'Consulta/diagnóstico', accion: 'confirmar' },
  { dia: 4, hora: '09:00', paciente: 6, prestacion: 'Control' },
  { dia: 4, hora: '10:30', paciente: 9, prestacion: 'Extracción', accion: 'confirmar' },
]

export const MOTIVO_CANCELACION = 'El paciente avisó que no puede venir'

const DIA_MS = 86_400_000

function partes(zona, instanteMs) {
  const formato = new Intl.DateTimeFormat('en-US', {
    timeZone: zona,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
  const p = {}
  for (const parte of formato.formatToParts(new Date(instanteMs))) {
    if (parte.type !== 'literal') p[parte.type] = Number(parte.value)
  }
  return p
}

function desplazamiento(zona, instanteMs) {
  const p = partes(zona, instanteMs)
  const comoUtc = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second)
  return Math.round((comoUtc - Math.floor(instanteMs / 1000) * 1000) / 60_000)
}

/** 'AAAA-MM-DD' + 'HH:mm' del consultorio → instante ISO con desplazamiento. */
export function instanteDe(fecha, hora, zona) {
  const [a, m, d] = fecha.split('-').map(Number)
  const [h, mi] = hora.split(':').map(Number)
  const comoUtc = Date.UTC(a, m - 1, d, h, mi)
  let offset = desplazamiento(zona, comoUtc)
  offset = desplazamiento(zona, comoUtc - offset * 60_000)
  const signo = offset < 0 ? '-' : '+'
  const abs = Math.abs(offset)
  const hh = String(Math.floor(abs / 60)).padStart(2, '0')
  const mm = String(abs % 60).padStart(2, '0')
  return `${fecha}T${hora}:00${signo}${hh}:${mm}`
}

export function hoyEn(zona, ahoraMs) {
  const p = partes(zona, ahoraMs)
  return `${p.year}-${String(p.month).padStart(2, '0')}-${String(p.day).padStart(2, '0')}`
}

export function sumarDias(fecha, dias) {
  const [a, m, d] = fecha.split('-').map(Number)
  return new Date(Date.UTC(a, m - 1, d) + dias * DIA_MS).toISOString().slice(0, 10)
}

/** 1 = lunes ... 7 = domingo, como diaSemana de los horarios. */
export function diaDeLaSemana(fecha) {
  const [a, m, d] = fecha.split('-').map(Number)
  const dia = new Date(Date.UTC(a, m - 1, d)).getUTCDay()
  return dia === 0 ? 7 : dia
}

const aMinutos = (hora) => {
  const [h, m] = hora.split(':').map(Number)
  return h * 60 + m
}

/** Proximos `cantidad` dias con franjas de atencion, empezando por hoy. */
export function diasConAtencion(hoy, franjas, cantidad) {
  const conAtencion = new Set(franjas.map((f) => f.diaSemana))
  const dias = []
  for (let i = 0; dias.length < cantidad && i < 21; i++) {
    const fecha = sumarDias(hoy, i)
    if (conAtencion.has(diaDeLaSemana(fecha))) dias.push(fecha)
  }
  return dias
}

/**
 * Turnos concretos a cargar: cada plantilla en su dia, solo si cae dentro de una franja,
 * la prestacion existe y empieza al menos `margenMin` minutos despues de ahora (la API rechaza el pasado).
 */
export function planDeTurnos({ zona, ahoraMs, franjas, prestaciones, desde, margenMin = 15 }) {
  const hoy = hoyEn(zona, ahoraMs)
  // Primer dia del plan: hoy o, si se pide, un dia posterior (p. ej. el de la demo).
  const primerDia = desde && desde > hoy ? desde : hoy
  const cantidad = Math.max(...TURNOS.map((t) => t.dia)) + 1
  const dias = diasConAtencion(primerDia, franjas, cantidad)
  const plan = []
  const omitidos = []
  for (const plantilla of TURNOS) {
    const fecha = dias[plantilla.dia]
    const prestacion = prestaciones.find((p) => p.activa && p.nombre === plantilla.prestacion)
    const motivo = !fecha
      ? 'sin día con atención'
      : !prestacion
        ? `no existe la prestación "${plantilla.prestacion}"`
        : null
    if (motivo) {
      omitidos.push({ ...plantilla, motivo })
      continue
    }
    const inicio = instanteDe(fecha, plantilla.hora, zona)
    const desde = aMinutos(plantilla.hora)
    const hasta = desde + prestacion.duracionMinutos
    const enFranja = franjas.some(
      (f) =>
        f.diaSemana === diaDeLaSemana(fecha) && desde >= aMinutos(f.desde) && hasta <= aMinutos(f.hasta),
    )
    if (!enFranja) {
      omitidos.push({ ...plantilla, motivo: 'fuera del horario de atención' })
    } else if (Date.parse(inicio) < ahoraMs + margenMin * 60_000) {
      omitidos.push({ ...plantilla, motivo: 'ya pasó' })
    } else {
      plan.push({
        ...plantilla,
        fecha,
        inicio,
        prestacionId: prestacion.id,
        sobreturno: Boolean(plantilla.sobreturno),
      })
    }
  }
  return { plan, omitidos }
}
