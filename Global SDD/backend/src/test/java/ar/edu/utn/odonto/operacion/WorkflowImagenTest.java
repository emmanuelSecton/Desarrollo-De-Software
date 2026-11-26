package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WorkflowImagenTest {

    private static final Path WORKFLOW = Path.of("..", "..", ".github", "workflows", "global-sdd-ci.yml")
            .toAbsolutePath()
            .normalize();

    @Test
    void elJobImagenEtiquetaConTagSemanticoYReportaElDigest() throws Exception {
        String job = jobImagen();
        assertThat(job).contains("docker build")
                .contains("odonto:ci")
                .contains("git describe")
                .contains("GITHUB_OUTPUT")
                .contains("docker image inspect")
                .contains("::notice");
    }

    @Test
    void elJobImagenNoHacePushLoginNiUsaSecretos() throws Exception {
        String job = jobImagen();
        assertThat(job).doesNotContain("docker push")
                .doesNotContain("docker/login-action")
                .doesNotContain("secrets.")
                .doesNotContain("registry");
    }

    @Test
    void losJobsBackendYFrontendQuedanIntactos() throws Exception {
        String workflow = Files.readString(WORKFLOW);
        assertThat(workflow).contains("  backend:")
                .contains("  frontend:")
                .contains("Build, tests, arquitectura y cobertura")
                .contains("npm run typecheck")
                .contains("npm test");
    }

    private static String jobImagen() throws Exception {
        String workflow = Files.readString(WORKFLOW);
        int inicio = workflow.indexOf("  imagen:");
        assertThat(inicio).isNotNegative();
        return workflow.substring(inicio);
    }
}
