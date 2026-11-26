package ar.edu.utn.odonto.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Usuarios ficticios para tests (SU-07): emails {@code *@consultorio.test} unicos por test, para no
 * chocar en la base compartida. Las {@code SECRETARIA} se pueden crear sin limite; el {@code ODONTOLOGO}
 * activo ya lo crea el alta inicial con las credenciales de {@code application-test.yml}.
 */
public final class UsuariosDePrueba {

    /** Cost bajo SOLO en datos de test: el codigo de produccion usa cost 12. */
    private static final BCryptPasswordEncoder CODIFICADOR_RAPIDO = new BCryptPasswordEncoder(4);

    public static final String CLAVE = "clave-de-prueba-123";

    public record Creado(UUID id, String email, String clave) {}

    private UsuariosDePrueba() {}

    public static String emailUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID() + "@consultorio.test";
    }

    public static Creado crear(JdbcTemplate jdbc, String rol, boolean activo, boolean debeCambiarPassword) {
        UUID id = UUID.randomUUID();
        String email = emailUnico(rol.toLowerCase());
        jdbc.update("insert into usuario (id, email, password_hash, nombre, rol, activo, debe_cambiar_password, "
                        + "creado_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                id, email, CODIFICADOR_RAPIDO.encode(CLAVE), "Usuario de prueba", rol, activo, debeCambiarPassword,
                Timestamp.from(Instant.parse("2026-11-25T12:00:00Z")));
        return new Creado(id, email, CLAVE);
    }

    public static Creado secretaria(JdbcTemplate jdbc) {
        return crear(jdbc, "SECRETARIA", true, false);
    }

    /** Crea un odontologo INACTIVO: el indice parcial permite varios inactivos. */
    public static Creado odontologoInactivo(JdbcTemplate jdbc) {
        return crear(jdbc, "ODONTOLOGO", false, false);
    }

    public static String cuerpoLogin(String email, String clave) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + clave + "\"}";
    }

    /** Inicia sesion por HTTP y devuelve la sesion para encadenar peticiones. */
    public static MockHttpSession login(MockMvc mvc, String email, String clave) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(email, clave)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }
}
