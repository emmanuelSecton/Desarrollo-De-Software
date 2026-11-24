package ar.edu.utn.odonto.arquitectura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Reglas de arquitectura aplicadas al codigo real de produccion (D2, D7). */
class ArquitecturaTest {

    private static JavaClasses produccion;

    @BeforeAll
    static void importar() {
        produccion = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ReglasArquitectura.BASE);
    }

    @Test
    void capasDeCadaModulo() {
        ReglasArquitectura.capasPorModulo(ReglasArquitectura.BASE, ReglasArquitectura.MODULOS)
                .forEach(r -> r.check(produccion));
    }

    @Test
    void soloLaApiPublicaEntreModulos() {
        ReglasArquitectura.apiPublicaEntreModulos(ReglasArquitectura.BASE, ReglasArquitectura.MODULOS)
                .forEach(r -> r.check(produccion));
    }

    @Test
    void sharedNoDependeDeModulos() {
        ReglasArquitectura.sharedSinModulos(ReglasArquitectura.BASE, ReglasArquitectura.MODULOS)
                .forEach(r -> r.check(produccion));
    }

    @Test
    void sinCiclosEntreModulos() {
        ReglasArquitectura.sinCiclos(ReglasArquitectura.BASE).check(produccion);
    }

    @Test
    void sinNowSinClock() {
        ReglasArquitectura.sinNowSinClock().check(produccion);
    }
}
