package ar.edu.utn.odonto.arquitectura;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Demuestra que cada regla de arquitectura RECHAZA su violacion y acepta una estructura conforme. */
class ReglasArquitecturaFixtureTest {

    private static final String FX = "ar.edu.utn.odonto.arquitectura.fixture.";
    private static final List<String> MODULOS = List.of("agenda", "pacientes");

    private static JavaClasses importar(String caso) {
        JavaClasses clases = new ClassFileImporter().importPackages(FX + caso);
        assertThat(clases).isNotEmpty();
        return clases;
    }

    /** Evalua TODAS las reglas y exige que alguna rechace la clase fixture nombrada en {@code fragmento}. */
    private static void assertViola(List<ArchRule> reglas, JavaClasses clases, String fragmento) {
        StringBuilder mensajes = new StringBuilder();
        for (ArchRule regla : reglas) {
            try {
                regla.check(clases);
            } catch (AssertionError e) {
                mensajes.append(e.getMessage()).append(System.lineSeparator());
            }
        }
        assertThat(mensajes.toString()).contains(fragmento);
    }

    @Test
    void domainNoPuedeUsarInfrastructure() {
        assertViola(ReglasArquitectura.capasPorModulo(FX + "capas", MODULOS), importar("capas"), "DomainUsaInfra");
    }

    @Test
    void applicationNoPuedeUsarApi() {
        assertViola(ReglasArquitectura.capasPorModulo(FX + "capas", MODULOS), importar("capas"), "ApplicationUsaApi");
    }

    @Test
    void applicationNoPuedeUsarInfrastructure() {
        assertViola(
                ReglasArquitectura.capasPorModulo(FX + "capas", MODULOS), importar("capas"), "ApplicationUsaInfra");
    }

    @Test
    void apiNoPuedeUsarInfrastructure() {
        assertViola(ReglasArquitectura.capasPorModulo(FX + "capas", MODULOS), importar("capas"), "ApiUsaInfra");
    }

    @Test
    void infrastructureNoPuedeUsarApi() {
        assertViola(ReglasArquitectura.capasPorModulo(FX + "capas", MODULOS), importar("capas"), "InfraUsaApi");
    }

    @Test
    void unModuloNoPuedeUsarDomainDeOtroModulo() {
        assertViola(
                ReglasArquitectura.apiPublicaEntreModulos(FX + "modulos", MODULOS),
                importar("modulos"),
                "TurnoUsaDomainAjeno");
    }

    @Test
    void sharedNoPuedeDependerDeUnModulo() {
        assertViola(
                ReglasArquitectura.sharedSinModulos(FX + "shared", MODULOS), importar("shared"), "SharedUsaModulo");
    }

    @Test
    void noPuedeHaberCiclosEntreModulos() {
        assertViola(List.of(ReglasArquitectura.sinCiclos(FX + "ciclo")), importar("ciclo"), "Cycle detected");
    }

    @Test
    void noSePermiteNowSinClock() {
        assertViola(List.of(ReglasArquitectura.sinNowSinClock()), importar("reloj"), "UsaNow");
    }

    @Test
    void nowConClockInyectadoEsValido() {
        JavaClasses clases = new ClassFileImporter()
                .importClasses(ar.edu.utn.odonto.arquitectura.fixture.reloj.UsaClockInyectado.class);
        ReglasArquitectura.sinNowSinClock().check(clases);
    }

    @Test
    void unaEstructuraConformeCumpleTodasLasReglas() {
        JavaClasses clases = importar("conforme");
        ReglasArquitectura.todas(FX + "conforme", MODULOS).forEach(r -> r.check(clases));
    }
}
