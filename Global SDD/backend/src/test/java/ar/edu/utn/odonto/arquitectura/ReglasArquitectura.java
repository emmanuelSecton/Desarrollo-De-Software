package ar.edu.utn.odonto.arquitectura;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.lang.ArchRule;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de arquitectura del monolito modular (D2, D7), parametrizadas por paquete base para poder
 * demostrar sobre fixtures que cada una rechaza su violacion.
 *
 * <p>La API publica de un modulo es su paquete {@code <modulo>.application}.
 */
final class ReglasArquitectura {

    static final String BASE = "ar.edu.utn.odonto";
    static final List<String> MODULOS = List.of(
            "agenda", "pacientes", "clinica", "caja", "notificaciones", "integraciones", "identidad", "auditoria");

    private ReglasArquitectura() {}

    private static String pkg(String base, String modulo, String capa) {
        return base + "." + modulo + "." + capa + "..";
    }

    /** Capas dentro de cada modulo: api -> application -> domain; infrastructure implementa puertos. */
    static List<ArchRule> capasPorModulo(String base, List<String> modulos) {
        List<ArchRule> reglas = new ArrayList<>();
        for (String m : modulos) {
            reglas.add(noClasses()
                    .that()
                    .resideInAPackage(pkg(base, m, "domain"))
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(pkg(base, m, "api"), pkg(base, m, "application"), pkg(base, m, "infrastructure"))
                    .as("domain de " + m + " no depende de api, application ni infrastructure"));
            reglas.add(noClasses()
                    .that()
                    .resideInAPackage(pkg(base, m, "application"))
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(pkg(base, m, "api"), pkg(base, m, "infrastructure"))
                    .as("application de " + m + " no depende de api ni infrastructure"));
            reglas.add(noClasses()
                    .that()
                    .resideInAPackage(pkg(base, m, "api"))
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(pkg(base, m, "infrastructure"))
                    .as("api de " + m + " no depende de infrastructure"));
            reglas.add(noClasses()
                    .that()
                    .resideInAPackage(pkg(base, m, "infrastructure"))
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(pkg(base, m, "api"))
                    .as("infrastructure de " + m + " no depende de api"));
        }
        return reglas;
    }

    /** Un modulo solo puede usar {@code application} de otro modulo (su API publica). */
    static List<ArchRule> apiPublicaEntreModulos(String base, List<String> modulos) {
        List<ArchRule> reglas = new ArrayList<>();
        for (String origen : modulos) {
            for (String destino : modulos) {
                if (origen.equals(destino)) {
                    continue;
                }
                reglas.add(noClasses()
                        .that()
                        .resideInAPackage(base + "." + origen + "..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                pkg(base, destino, "api"),
                                pkg(base, destino, "domain"),
                                pkg(base, destino, "infrastructure"))
                        .as(origen + " no usa clases internas (api, domain, infrastructure) de " + destino));
            }
        }
        return reglas;
    }

    /** {@code shared} no depende de ningun modulo de dominio. */
    static List<ArchRule> sharedSinModulos(String base, List<String> modulos) {
        String[] paquetes = modulos.stream().map(m -> base + "." + m + "..").toArray(String[]::new);
        return List.of(noClasses()
                .that()
                .resideInAPackage(base + ".shared..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(paquetes)
                .as("shared no depende de ningun modulo de dominio"));
    }

    /** Sin ciclos entre modulos (cada subpaquete directo del paquete base es un slice). */
    static ArchRule sinCiclos(String base) {
        return slices().matching(base + ".(*)..").should().beFreeOfCycles();
    }

    /** Prohibe {@code now()} sin argumentos de java.time: la hora siempre sale del {@code Clock} inyectado. */
    static ArchRule sinNowSinClock() {
        DescribedPredicate<JavaCall<?>> nowSinClock = target(name("now"))
                .and(describe("sin parametros", (JavaCall<?> c) -> c.getTarget().getRawParameterTypes().isEmpty()))
                .and(describe(
                        "de java.time",
                        (JavaCall<?> c) -> c.getTargetOwner().getPackageName().equals("java.time")));
        return noClasses()
                .should()
                .callMethodWhere(nowSinClock)
                .as("nadie llama a java.time.*.now() sin Clock: usar el Clock inyectado");
    }

    static List<ArchRule> todas(String base, List<String> modulos) {
        List<ArchRule> reglas = new ArrayList<>();
        reglas.addAll(capasPorModulo(base, modulos));
        reglas.addAll(apiPublicaEntreModulos(base, modulos));
        reglas.addAll(sharedSinModulos(base, modulos));
        reglas.add(sinCiclos(base));
        reglas.add(sinNowSinClock());
        return reglas;
    }
}
