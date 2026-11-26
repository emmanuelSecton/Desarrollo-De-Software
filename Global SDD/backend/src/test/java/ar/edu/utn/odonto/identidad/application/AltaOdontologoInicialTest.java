package ar.edu.utn.odonto.identidad.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.identidad.domain.Rol;
import ar.edu.utn.odonto.identidad.domain.Usuario;
import ar.edu.utn.odonto.identidad.infrastructure.InicializadorOdontologo;
import ar.edu.utn.odonto.identidad.infrastructure.PropiedadesIdentidad;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** Alta del odontologo inicial (D2): base real para el caso normal, puerto en memoria para la base vacia. */
class AltaOdontologoInicialTest {

    private static final String EMAIL = "dra.inicial@consultorio.test";
    private static final String CLAVE = "clave-temporal-secreta-9";

    /** Puerto en memoria: simula una instancia vacia sin depender del estado de la base compartida. */
    static class RepositorioEnMemoria implements RepositorioUsuarios {
        final List<Usuario> usuarios = new ArrayList<>();

        @Override
        public Optional<Usuario> buscarPorEmailParaActualizar(String email) {
            return usuarios.stream().filter(u -> u.email().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorId(UUID id) {
            return usuarios.stream().filter(u -> u.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorIdParaActualizar(UUID id) {
            return buscarPorId(id);
        }

        @Override
        public boolean existeOdontologo() {
            return usuarios.stream().anyMatch(u -> u.rol() == Rol.ODONTOLOGO);
        }

        @Override
        public void insertar(Usuario usuario) {
            usuarios.add(usuario);
        }

        @Override
        public void actualizar(Usuario usuario) {}
    }

    private static AltaOdontologoInicial alta(RepositorioEnMemoria repositorio) {
        return new AltaOdontologoInicial(
                repositorio,
                new BCryptPasswordEncoder(4),
                Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC),
                evento -> {});
    }

    @Test
    void enUnaInstanciaVaciaCreaElOdontologoConCambioObligatorio() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();

        assertThat(alta(repositorio).crearSiFalta(" " + EMAIL + " ", CLAVE)).isTrue();

        Usuario creado = repositorio.usuarios.get(0);
        assertThat(creado.email()).isEqualTo(EMAIL);
        assertThat(creado.rol()).isEqualTo(Rol.ODONTOLOGO);
        assertThat(creado.activo()).isTrue();
        assertThat(creado.debeCambiarPassword()).isTrue();
        assertThat(creado.passwordHash()).doesNotContain(CLAVE).startsWith("$2a$");
        assertThat(creado.creadoAt()).isEqualTo(Instant.parse("2026-11-25T12:00:00Z"));
    }

    @Test
    void sinEmailFallaNombrandoLaVariableSinMostrarValores() {
        assertThatThrownBy(() -> alta(new RepositorioEnMemoria()).crearSiFalta("  ", CLAVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_INITIAL_EMAIL")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(CLAVE));
    }

    @Test
    void sinClaveFallaNombrandoLaVariableSinMostrarValores() {
        assertThatThrownBy(() -> alta(new RepositorioEnMemoria()).crearSiFalta(EMAIL, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_INITIAL_PASSWORD")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(EMAIL));
    }

    @Test
    void conUnOdontologoExistenteIgnoraLasVariablesAunqueFalten() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        alta(repositorio).crearSiFalta(EMAIL, CLAVE);

        assertThat(alta(repositorio).crearSiFalta(null, null)).isFalse();
        assertThat(repositorio.usuarios).hasSize(1);
    }

    @Test
    void elInicializadorNoLogueaNiElEmailNiLaClave() {
        Logger logger = (Logger) LoggerFactory.getLogger(InicializadorOdontologo.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new InicializadorOdontologo(
                            alta(new RepositorioEnMemoria()),
                            new PropiedadesIdentidad(EMAIL, CLAVE, Duration.ofMinutes(30), 10, 60))
                    .run(null);

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining(" "));
            assertThat(logs).contains("odontólogo inicial").doesNotContain(EMAIL).doesNotContain(CLAVE);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void lasPropiedadesNoExponenLasCredencialesEnToString() {
        assertThat(new PropiedadesIdentidad(EMAIL, CLAVE, Duration.ofMinutes(30), 10, 60).toString())
                .doesNotContain(EMAIL)
                .doesNotContain(CLAVE);
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(PostgresTestConfig.class)
    class ConBaseReal {

        @Autowired
        DataSource dataSource;

        @Autowired
        AltaOdontologoInicial alta;

        @Test
        void elArranqueCreoElOdontologoDeApplicationTestConBcryptCost12() {
            Map<String, Object> fila = new JdbcTemplate(dataSource).queryForMap(
                    "select rol, activo, debe_cambiar_password, password_hash from usuario where email = ?",
                    "odontologo.inicial@consultorio.test");

            assertThat(fila.get("rol")).isEqualTo("ODONTOLOGO");
            assertThat(fila.get("activo")).isEqualTo(true);
            assertThat(fila.get("debe_cambiar_password")).isEqualTo(true);
            assertThat((String) fila.get("password_hash")).startsWith("$2a$12$").hasSize(60);
        }

        @Test
        void unNuevoArranqueNoCreaNiModificaUsuarios() {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Integer antes = jdbc.queryForObject("select count(*) from usuario", Integer.class);

            assertThat(alta.crearSiFalta("otro@consultorio.test", "otra-clave-ficticia")).isFalse();
            assertThat(jdbc.queryForObject("select count(*) from usuario", Integer.class)).isEqualTo(antes);
        }
    }
}
