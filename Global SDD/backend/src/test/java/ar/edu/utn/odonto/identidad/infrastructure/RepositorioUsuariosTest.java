package ar.edu.utn.odonto.identidad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.identidad.application.RepositorioUsuarios;
import ar.edu.utn.odonto.identidad.domain.Rol;
import ar.edu.utn.odonto.identidad.domain.Usuario;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JDBC de usuario contra PostgreSQL 16 real (C-03). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
@Transactional
class RepositorioUsuariosTest {

    private static final Instant AHORA = Instant.parse("2026-11-25T12:00:00Z");

    @Autowired
    RepositorioUsuarios repositorio;

    @Test
    void guardaYBuscaPorEmailSinDistinguirMayusculas() {
        String email = UsuariosDePrueba.emailUnico("Repo");
        Usuario usuario = Usuario.nuevo(email, "hash", "Secretaria", Rol.SECRETARIA, AHORA);

        repositorio.insertar(usuario);

        Usuario leido = repositorio.buscarPorEmailParaActualizar(email.toUpperCase()).orElseThrow();
        assertThat(leido.id()).isEqualTo(usuario.id());
        assertThat(leido.rol()).isEqualTo(Rol.SECRETARIA);
        assertThat(leido.creadoAt()).isEqualTo(AHORA);
        assertThat(leido.debeCambiarPassword()).isTrue();
    }

    @Test
    void persisteContadoresBloqueoYUltimoLogin() {
        Usuario usuario = Usuario.nuevo(UsuariosDePrueba.emailUnico("repo"), "hash", "S", Rol.SECRETARIA, AHORA);
        repositorio.insertar(usuario);

        Usuario paraActualizar = repositorio.buscarPorIdParaActualizar(usuario.id()).orElseThrow();
        for (int i = 0; i < Usuario.MAX_INTENTOS; i++) {
            paraActualizar.registrarFallo(AHORA.plusSeconds(i));
        }
        paraActualizar.cambiarPassword("otro-hash");
        repositorio.actualizar(paraActualizar);

        Usuario leido = repositorio.buscarPorId(usuario.id()).orElseThrow();
        assertThat(leido.bloqueadoHasta()).isEqualTo(paraActualizar.bloqueadoHasta());
        assertThat(leido.passwordHash()).isEqualTo("otro-hash");
        assertThat(leido.debeCambiarPassword()).isFalse();

        leido.registrarExito(AHORA.plusSeconds(3600));
        repositorio.actualizar(leido);
        assertThat(repositorio.buscarPorId(usuario.id()).orElseThrow().ultimoLoginAt())
                .isEqualTo(AHORA.plusSeconds(3600));
    }

    @Test
    void unIdOEmailInexistenteDevuelveVacio() {
        assertThat(repositorio.buscarPorId(UUID.randomUUID())).isEmpty();
        assertThat(repositorio.buscarPorEmailParaActualizar("nadie@consultorio.test")).isEmpty();
    }

    @Test
    void existeOdontologoPorElAltaInicial() {
        assertThat(repositorio.existeOdontologo()).isTrue();
    }
}
