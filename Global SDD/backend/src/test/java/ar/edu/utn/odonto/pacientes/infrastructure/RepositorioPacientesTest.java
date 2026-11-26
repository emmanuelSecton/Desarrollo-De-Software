package ar.edu.utn.odonto.pacientes.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.pacientes.application.CriterioDeBusqueda;
import ar.edu.utn.odonto.pacientes.application.Pagina;
import ar.edu.utn.odonto.pacientes.application.RepositorioPacientes;
import ar.edu.utn.odonto.pacientes.domain.Paciente;
import ar.edu.utn.odonto.support.PacientesDePrueba;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JDBC de paciente y su busqueda (D5, D6) contra PostgreSQL 16 real. */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
@Transactional
class RepositorioPacientesTest {

    private static final Instant AHORA = Instant.parse("2026-11-26T12:00:00Z");

    @Autowired
    RepositorioPacientes repositorio;

    private Paciente guardar(String nombre, String apellido) {
        Paciente paciente = Paciente.nuevo(new Paciente.DatosPersonales(PacientesDePrueba.dniUnico(), nombre, apellido,
                LocalDate.of(1985, 1, 2), PacientesDePrueba.celularUnico(), null, false), "v1", AHORA);
        assertThat(repositorio.insertarSiNoExiste(paciente)).isTrue();
        return paciente;
    }

    private Pagina<Paciente> buscar(String q, boolean incluirInactivos) {
        return repositorio.buscar(CriterioDeBusqueda.de(q), incluirInactivos, 0, 20);
    }

    @Test
    void guardaYLeeTodosLosCampos() {
        Paciente guardado = guardar("Ana", "Pérez " + PacientesDePrueba.sufijo());

        Paciente leido = repositorio.buscarPorId(guardado.id()).orElseThrow();
        assertThat(leido.datos().dni()).isEqualTo(guardado.datos().dni());
        assertThat(leido.datos().fechaNacimiento()).isEqualTo(LocalDate.of(1985, 1, 2));
        assertThat(leido.consentimientoDatosAt()).isEqualTo(AHORA);
        assertThat(leido.politicaPrivacidadVersion()).isEqualTo("v1");
        assertThat(repositorio.buscarPorDni(guardado.datos().dni())).isPresent();
        assertThat(repositorio.buscarPorId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void unSegundoPacienteConElMismoDniNoSeInserta() {
        Paciente original = guardar("Ana", "Pérez");
        Paciente copia = Paciente.nuevo(original.datos(), "v1", AHORA);

        assertThat(repositorio.insertarSiNoExiste(copia)).isFalse();
        assertThat(repositorio.buscarPorDni(original.datos().dni()).orElseThrow().id()).isEqualTo(original.id());
    }

    @Test
    void buscaPorApellidoSinAcentosNiMayusculas() {
        String sufijo = PacientesDePrueba.sufijo();
        Paciente ana = guardar("Ana", "Pérez " + sufijo);

        assertThat(buscar("PEREZ " + sufijo, false).contenido()).extracting(Paciente::id).containsExactly(ana.id());
        assertThat(buscar(sufijo + " ana", false).contenido()).extracting(Paciente::id).containsExactly(ana.id());
    }

    @Test
    void buscaPorDniYPorTelefonoEnCualquierFormato() {
        Paciente paciente = guardar("Luis", "Gómez " + PacientesDePrueba.sufijo());
        String telefono = paciente.datos().telefonoE164();
        // "+549" + "11" + 8 digitos: el numero nacional sin el 9 de celular.
        String nacional = telefono.substring(4);

        assertThat(buscar(paciente.datos().dni(), false).contenido()).extracting(Paciente::id).contains(paciente.id());
        assertThat(buscar(telefono, false).contenido()).extracting(Paciente::id).containsExactly(paciente.id());
        assertThat(buscar("011 15 " + nacional.substring(2), false).contenido())
                .extracting(Paciente::id).containsExactly(paciente.id());
    }

    @Test
    void losInactivosSoloAparecenSiSePiden() {
        String sufijo = PacientesDePrueba.sufijo();
        Paciente activo = guardar("Eva", "Ruiz " + sufijo);
        Paciente inactivo = guardar("Ema", "Ruiz " + sufijo);
        inactivo.darDeBaja(AHORA);
        repositorio.actualizar(inactivo);

        assertThat(buscar(sufijo, false).contenido()).extracting(Paciente::id).containsExactly(activo.id());
        assertThat(buscar(sufijo, true).contenido()).extracting(Paciente::id)
                .containsExactlyInAnyOrder(activo.id(), inactivo.id());
    }

    @Test
    void paginaOrdenaPorApellidoYNombreYCuentaElTotal() {
        String sufijo = PacientesDePrueba.sufijo();
        guardar("Bea", "Zeta " + sufijo);
        guardar("Abel", "Alfa " + sufijo);
        guardar("Carla", "Alfa " + sufijo);

        Pagina<Paciente> primera = repositorio.buscar(CriterioDeBusqueda.de(sufijo), false, 0, 2);
        Pagina<Paciente> segunda = repositorio.buscar(CriterioDeBusqueda.de(sufijo), false, 1, 2);

        assertThat(primera.total()).isEqualTo(3);
        assertThat(primera.contenido()).extracting(p -> p.datos().nombre()).containsExactly("Abel", "Carla");
        assertThat(segunda.contenido()).extracting(p -> p.datos().nombre()).containsExactly("Bea");
    }

    @Test
    void losComodinesDelUsuarioSeBuscanLiteralmente() {
        guardar("Ana", "Pérez " + PacientesDePrueba.sufijo());

        assertThat(buscar("%", false).contenido()).isEmpty();
        assertThat(buscar("_", false).contenido()).isEmpty();
    }
}
