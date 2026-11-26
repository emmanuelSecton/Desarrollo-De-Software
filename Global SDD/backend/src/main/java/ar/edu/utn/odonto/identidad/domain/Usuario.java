package ar.edu.utn.odonto.identidad.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuario interno con la logica de bloqueo por intentos fallidos (RN-SEG-03, D5).
 * Recibe siempre el instante actual: nunca consulta el reloj por su cuenta.
 */
public class Usuario {

    public static final int MAX_INTENTOS = 5;
    public static final Duration VENTANA_DE_FALLOS = Duration.ofMinutes(15);
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final UUID id;
    private final String email;
    private String passwordHash;
    private final String nombre;
    private final Rol rol;
    private final boolean activo;
    private boolean debeCambiarPassword;
    private int intentosFallidos;
    private Instant primerFalloAt;
    private Instant bloqueadoHasta;
    private Instant ultimoLoginAt;
    private final Instant creadoAt;

    public Usuario(
            UUID id,
            String email,
            String passwordHash,
            String nombre,
            Rol rol,
            boolean activo,
            boolean debeCambiarPassword,
            int intentosFallidos,
            Instant primerFalloAt,
            Instant bloqueadoHasta,
            Instant ultimoLoginAt,
            Instant creadoAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.activo = activo;
        this.debeCambiarPassword = debeCambiarPassword;
        this.intentosFallidos = intentosFallidos;
        this.primerFalloAt = primerFalloAt;
        this.bloqueadoHasta = bloqueadoHasta;
        this.ultimoLoginAt = ultimoLoginAt;
        this.creadoAt = Objects.requireNonNull(creadoAt, "creadoAt");
    }

    /** Alta de un usuario nuevo: activo, sin fallos y con cambio de contraseña obligatorio. */
    public static Usuario nuevo(String email, String passwordHash, String nombre, Rol rol, Instant ahora) {
        return new Usuario(UUID.randomUUID(), email, passwordHash, nombre, rol, true, true, 0, null, null, null, ahora);
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Cuenta un fallo dentro de la ventana de 15 min; al quinto, bloquea la cuenta 15 min. */
    public void registrarFallo(Instant ahora) {
        if (primerFalloAt == null || !ahora.isBefore(primerFalloAt.plus(VENTANA_DE_FALLOS))) {
            primerFalloAt = ahora;
            intentosFallidos = 1;
        } else {
            intentosFallidos++;
        }
        if (intentosFallidos >= MAX_INTENTOS) {
            bloqueadoHasta = ahora.plus(DURACION_BLOQUEO);
            intentosFallidos = 0;
            primerFalloAt = null;
        }
    }

    public void registrarExito(Instant ahora) {
        intentosFallidos = 0;
        primerFalloAt = null;
        bloqueadoHasta = null;
        ultimoLoginAt = ahora;
    }

    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = Objects.requireNonNull(nuevoHash, "nuevoHash");
        this.debeCambiarPassword = false;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String nombre() {
        return nombre;
    }

    public Rol rol() {
        return rol;
    }

    public boolean activo() {
        return activo;
    }

    public boolean debeCambiarPassword() {
        return debeCambiarPassword;
    }

    public int intentosFallidos() {
        return intentosFallidos;
    }

    public Instant primerFalloAt() {
        return primerFalloAt;
    }

    public Instant bloqueadoHasta() {
        return bloqueadoHasta;
    }

    public Instant ultimoLoginAt() {
        return ultimoLoginAt;
    }

    public Instant creadoAt() {
        return creadoAt;
    }
}
