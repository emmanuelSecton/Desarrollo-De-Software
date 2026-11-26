package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RolloutCanarioTest {

    @Test
    void ambosCanariosSanosPermitenContinuarAlResto() {
        Map<String, Boolean> salud = new LinkedHashMap<>();
        salud.put("demo_uno", true);
        salud.put("demo_dos", true);

        PlanRollout.Resultado resultado = PlanRollout.evaluar(salud, List.of("consultorio_tres", "consultorio_cuatro"));

        assertThat(resultado.continuar()).isTrue();
        assertThat(resultado.destinos()).containsExactly("consultorio_tres", "consultorio_cuatro");
        assertThat(resultado.motivo()).isEmpty();
    }

    @Test
    void unaCanariaDownDetieneElRolloutAntesDeTocarLasDemas() {
        Map<String, Boolean> salud = new LinkedHashMap<>();
        salud.put("demo_uno", true);
        salud.put("demo_dos", false);

        PlanRollout.Resultado resultado = PlanRollout.evaluar(salud, List.of("consultorio_tres"));

        assertThat(resultado.continuar()).isFalse();
        assertThat(resultado.destinos()).isEmpty();
        assertThat(resultado.motivo()).contains("demo_dos");
    }

    @Test
    void unaCanariaSinHealthSeTrataComoFallo() {
        PlanRollout.Resultado resultado =
                PlanRollout.evaluar(Map.of("demo_uno", true), List.of("consultorio_tres"));

        assertThat(resultado.continuar()).isFalse();
        assertThat(resultado.destinos()).isEmpty();
        assertThat(resultado.motivo()).contains("faltan");
    }

    @Test
    void elScriptActualizaPrimeroLosDosCanarios() throws Exception {
        String contenido = Files.readString(Path.of("..", "deploy", "bin", "rollout-canario.sh")
                .toAbsolutePath()
                .normalize());
        assertThat(contenido).contains("demo_uno")
                .contains("demo_dos")
                .contains("/actuator/health")
                .contains("CONSULTORIO_SLUGS");
        assertThat(contenido.indexOf("demo_uno")).isLessThan(contenido.indexOf("CONSULTORIO_SLUGS"));
    }
}
