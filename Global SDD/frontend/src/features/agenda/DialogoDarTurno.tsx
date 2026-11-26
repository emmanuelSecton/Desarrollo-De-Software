import { Layers, UserPlus } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ErrorDeApi } from '../../shared/api-client/cliente'
import { instanteDe, type Fecha } from '../../shared/formatos/fechas'
import { Alerta } from '../../shared/ui/Alerta'
import { Boton } from '../../shared/ui/Boton'
import { Campo } from '../../shared/ui/Campo'
import { Dialogo } from '../../shared/ui/Dialogo'
import { Selector } from '../../shared/ui/Selector'
import {
  buscarPacientes,
  crearPaciente,
  crearTurno,
  PROBLEMAS_DE_TURNOS,
  type Paciente,
  type Turno,
} from './api'
import {
  CamposDeHorario,
  describirChoques,
  erroresPorCampo,
  esProblema,
  fueraDeAtencion,
  validarHorario,
  type ErroresDeHorario,
} from './formulario'
import estilos from './Formulario.module.css'
import type { ConfiguracionDeAgenda } from './useAgenda'

interface Props {
  fecha: Fecha
  hora?: string
  configuracion: ConfiguracionDeAgenda
  /** Turnos ya cargados: para decir con cual choca un horario ocupado. */
  turnos: Turno[]
  alListo: (fecha: Fecha, mensaje: string) => void
  alCerrar: () => void
}

interface Errores extends ErroresDeHorario {
  paciente?: string
  prestacion?: string
}

const ERROR_SIN_CONEXION =
  'No pudimos conectar con el consultorio. Probá de nuevo en unos segundos.'

/** Dar un turno: buscar o dar de alta al paciente, elegir prestacion, dia, hora y duracion (US-011). */
export function DialogoDarTurno({
  fecha: fechaInicial,
  hora: horaInicial,
  configuracion,
  turnos,
  alListo,
  alCerrar,
}: Props) {
  const { prestaciones, franjas, zona } = configuracion
  const [paciente, setPaciente] = useState<Paciente | null>(null)
  const [nuevo, setNuevo] = useState(false)
  const [prestacionId, setPrestacionId] = useState(prestaciones[0]?.id ?? '')
  const prestacion = prestaciones.find((p) => p.id === prestacionId)
  const [fecha, setFecha] = useState(fechaInicial)
  const [hora, setHora] = useState(horaInicial ?? '')
  const [duracion, setDuracion] = useState(prestacion?.duracionMinutos ?? 30)
  const [errores, setErrores] = useState<Errores>({})
  const [choques, setChoques] = useState<string[] | null>(null)
  const [falla, setFalla] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const refHora = useRef<HTMLInputElement>(null)

  function cambiarPrestacion(id: string) {
    setPrestacionId(id)
    const elegida = prestaciones.find((p) => p.id === id)
    if (elegida) setDuracion(elegida.duracionMinutos)
  }

  async function enviar(sobreturno: boolean) {
    const nuevos: Errores = validarHorario(fecha, hora)
    if (!paciente) nuevos.paciente = 'Elegí al paciente o dalo de alta'
    if (!prestacion) nuevos.prestacion = 'Elegí la prestación'
    setErrores(nuevos)
    setFalla(null)
    if (Object.keys(nuevos).length > 0 || !paciente || !prestacion) return

    setEnviando(true)
    try {
      await crearTurno({
        pacienteId: paciente.id,
        prestacionId: prestacion.id,
        inicio: instanteDe(fecha, hora, zona),
        duracionMinutos: duracion,
        sobreturno,
      })
      alListo(fecha, sobreturno ? 'Sobreturno dado' : 'Turno dado')
    } catch (error) {
      setEnviando(false)
      if (esProblema(error, PROBLEMAS_DE_TURNOS.superpuesto)) {
        setChoques(describirChoques(turnos, fecha, hora, duracion, zona))
        return
      }
      if (error instanceof ErrorDeApi && error.errores.length > 0) {
        setErrores(
          erroresPorCampo(error, {
            inicio: 'hora',
            duracionMinutos: 'duracion',
            pacienteId: 'paciente',
            prestacionId: 'prestacion',
          }),
        )
        return
      }
      setFalla(error instanceof ErrorDeApi ? error.detalle || error.titulo : ERROR_SIN_CONEXION)
    }
  }

  function alEnviar(evento: FormEvent) {
    evento.preventDefault()
    void enviar(false)
  }

  const avisoFuera = fueraDeAtencion(franjas, fecha, hora, duracion)

  return (
    <Dialogo
      abierto
      ancho
      conCerrar
      describir={false}
      titulo="Dar turno"
      alCancelar={alCerrar}
      acciones={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" form="form-dar-turno" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Dar turno'}
          </Boton>
        </>
      }
    >
      <section className={estilos.seccion} aria-labelledby="titulo-paciente">
        <h3 id="titulo-paciente">{nuevo ? 'Paciente nuevo' : 'Paciente'}</h3>
        {paciente ? (
          <div className={estilos.seleccion}>
            <span className={estilos.datosPaciente}>
              <strong>
                {paciente.nombre} {paciente.apellido}
              </strong>
              <small>
                DNI {paciente.dni} · {paciente.telefono}
              </small>
            </span>
            <Boton variante="fantasma" onClick={() => setPaciente(null)}>
              Cambiar
            </Boton>
          </div>
        ) : nuevo ? (
          <AltaDePaciente
            alCrear={(creado) => {
              setPaciente(creado)
              setNuevo(false)
              setErrores((e) => ({ ...e, paciente: undefined }))
            }}
            alVolver={() => setNuevo(false)}
          />
        ) : (
          <BuscadorDePaciente
            error={errores.paciente}
            alElegir={(elegido) => {
              setPaciente(elegido)
              setErrores((e) => ({ ...e, paciente: undefined }))
            }}
            alNuevo={() => setNuevo(true)}
          />
        )}
      </section>

      <form
        id="form-dar-turno"
        className={estilos.seccion}
        noValidate
        onSubmit={alEnviar}
        aria-labelledby="titulo-turno"
      >
        <h3 id="titulo-turno">Turno</h3>
        <Selector
          etiqueta="Prestación"
          opciones={prestaciones.map((p) => ({
            valor: p.id,
            texto: `${p.nombre} (${p.duracionMinutos} min)`,
          }))}
          value={prestacionId}
          error={errores.prestacion}
          onChange={(e) => cambiarPrestacion(e.target.value)}
        />
        <CamposDeHorario
          fecha={fecha}
          hora={hora}
          duracion={duracion}
          duracionMinima={prestacion?.duracionMinutos ?? 5}
          errores={errores}
          refHora={refHora}
          alCambiar={(cambio) => {
            if (cambio.fecha !== undefined) setFecha(cambio.fecha)
            if (cambio.hora !== undefined) setHora(cambio.hora)
            if (cambio.duracion !== undefined) setDuracion(cambio.duracion)
            setChoques(null)
          }}
        />
        {avisoFuera && (
          <Alerta tipo="aviso">
            Ese horario está fuera de la atención del día. Podés darlo igual.
          </Alerta>
        )}
      </form>

      {choques && (
        <Alerta tipo="error" titulo="Ese horario se superpone con otro turno">
          {choques.length > 0 ? `${choques.join('; ')}. ` : ''}
          Elegí otro horario o, si hace falta atenderlo igual, dalo como sobreturno.
          <span className={estilos.accionesAlerta}>
            <Boton
              variante="secundario"
              onClick={() => {
                setChoques(null)
                refHora.current?.focus()
              }}
            >
              Elegir otro horario
            </Boton>
            <Boton onClick={() => void enviar(true)} disabled={enviando}>
              <Layers aria-hidden="true" />
              Dar como sobreturno
            </Boton>
          </span>
        </Alerta>
      )}
      {falla && <Alerta tipo="error">{falla}</Alerta>}
    </Dialogo>
  )
}

function BuscadorDePaciente({
  error,
  alElegir,
  alNuevo,
}: {
  error?: string
  alElegir: (paciente: Paciente) => void
  alNuevo: () => void
}) {
  const [texto, setTexto] = useState('')
  const [resultados, setResultados] = useState<Paciente[] | null>(null)
  const consulta = texto.trim()

  useEffect(() => {
    if (consulta.length < 2) return
    let vigente = true
    const espera = setTimeout(() => {
      buscarPacientes(consulta)
        .then((pagina) => vigente && setResultados(pagina.contenido.filter((p) => p.activo)))
        .catch(() => vigente && setResultados([]))
    }, 300)
    return () => {
      vigente = false
      clearTimeout(espera)
    }
  }, [consulta])

  const visibles = consulta.length < 2 ? null : resultados

  return (
    <>
      <Campo
        etiqueta="Buscar por DNI, nombre o teléfono"
        type="search"
        autoComplete="off"
        value={texto}
        error={error}
        data-foco-inicial
        onChange={(e) => setTexto(e.target.value)}
      />
      {visibles && (
        <ul className={estilos.resultados} aria-label="Pacientes encontrados">
          {visibles.length === 0 && (
            <li className={estilos.busqueda}>No encontramos pacientes con “{consulta}”.</li>
          )}
          {visibles.map((p) => (
            <li key={p.id}>
              <button type="button" className={estilos.resultado} onClick={() => alElegir(p)}>
                <span className={estilos.datosPaciente}>
                  <strong>
                    {p.nombre} {p.apellido}
                  </strong>
                  <small>
                    DNI {p.dni} · {p.telefono}
                  </small>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <Boton variante="secundario" onClick={alNuevo}>
        <UserPlus aria-hidden="true" />
        Nuevo paciente
      </Boton>
    </>
  )
}

interface DatosNuevo {
  dni: string
  nombre: string
  apellido: string
  telefono: string
  email: string
}

type ErroresNuevo = Partial<Record<keyof DatosNuevo | 'consentimientoDatos', string>>

function AltaDePaciente({
  alCrear,
  alVolver,
}: {
  alCrear: (paciente: Paciente) => void
  alVolver: () => void
}) {
  const [datos, setDatos] = useState<DatosNuevo>({
    dni: '',
    nombre: '',
    apellido: '',
    telefono: '',
    email: '',
  })
  const [whatsapp, setWhatsapp] = useState(true)
  const [consentimiento, setConsentimiento] = useState(false)
  const [errores, setErrores] = useState<ErroresNuevo>({})
  const [falla, setFalla] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  const campo = (clave: keyof DatosNuevo, etiqueta: string, extra: object = {}) => (
    <Campo
      etiqueta={etiqueta}
      value={datos[clave]}
      error={errores[clave]}
      onChange={(e) => setDatos((d) => ({ ...d, [clave]: e.target.value }))}
      {...extra}
    />
  )

  async function guardar() {
    const faltan: ErroresNuevo = {}
    if (!datos.dni.trim()) faltan.dni = 'Ingresá el DNI'
    if (!datos.nombre.trim()) faltan.nombre = 'Ingresá el nombre'
    if (!datos.apellido.trim()) faltan.apellido = 'Ingresá el apellido'
    if (!datos.telefono.trim()) faltan.telefono = 'Ingresá el teléfono'
    if (!consentimiento) {
      faltan.consentimientoDatos =
        'Sin la aceptación del tratamiento de datos no se puede dar de alta'
    }
    setErrores(faltan)
    setFalla(null)
    if (Object.keys(faltan).length > 0) return

    setGuardando(true)
    try {
      const creado = await crearPaciente({
        dni: datos.dni.trim(),
        nombre: datos.nombre.trim(),
        apellido: datos.apellido.trim(),
        telefono: datos.telefono.trim(),
        email: datos.email.trim() || null,
        aceptaWhatsapp: whatsapp,
        consentimientoDatos: true,
      })
      alCrear(creado)
    } catch (error) {
      setGuardando(false)
      if (esProblema(error, PROBLEMAS_DE_TURNOS.pacienteDuplicado)) {
        setErrores({ dni: 'Ya hay un paciente con ese DNI: buscalo para darle el turno' })
      } else if (error instanceof ErrorDeApi && error.errores.length > 0) {
        setErrores(erroresPorCampo(error))
      } else {
        setFalla(error instanceof ErrorDeApi ? error.detalle || error.titulo : ERROR_SIN_CONEXION)
      }
    }
  }

  return (
    <>
      <div className={estilos.fila}>
        {campo('dni', 'DNI', { inputMode: 'numeric', 'data-foco-inicial': true })}
        {campo('nombre', 'Nombre')}
        {campo('apellido', 'Apellido')}
      </div>
      <div className={estilos.fila}>
        {campo('telefono', 'Teléfono', { type: 'tel', autoComplete: 'off' })}
        {campo('email', 'Email (opcional)', { type: 'email', autoComplete: 'off' })}
      </div>
      <label className={estilos.casilla}>
        <input type="checkbox" checked={whatsapp} onChange={(e) => setWhatsapp(e.target.checked)} />
        Acepta recibir recordatorios por WhatsApp
      </label>
      <label className={estilos.casilla}>
        <input
          type="checkbox"
          checked={consentimiento}
          aria-invalid={errores.consentimientoDatos ? true : undefined}
          onChange={(e) => setConsentimiento(e.target.checked)}
        />
        El paciente aceptó la política de privacidad y el tratamiento de sus datos de salud
      </label>
      {errores.consentimientoDatos && <Alerta tipo="error">{errores.consentimientoDatos}</Alerta>}
      {falla && <Alerta tipo="error">{falla}</Alerta>}
      <span className={estilos.accionesAlerta}>
        <Boton variante="secundario" onClick={alVolver}>
          Volver a buscar
        </Boton>
        <Boton onClick={() => void guardar()} disabled={guardando}>
          {guardando ? 'Guardando…' : 'Guardar paciente'}
        </Boton>
      </span>
    </>
  )
}
