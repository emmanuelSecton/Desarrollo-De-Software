package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecretosFueraRepoTest {

    @Test
    void elEjemploDeclaraLasVariablesRequeridasSinValoresReales() throws Exception {
        String ejemplo = Files.readString(Path.of("..", ".env.example").toAbsolutePath().normalize());
        assertThat(ejemplo).contains("DB_POOL_MAX_SIZE=5")
                .contains("ODONTO_ENV_DIR=../instances")
                .contains("CONSULTORIO_SLUGS=demo_uno demo_dos")
                .contains("ADMIN_INITIAL_EMAIL=")
                .contains("WA_VERIFY_TOKEN=")
                .contains("DB_PASSWORD=")
                .contains("APP_DB_MIGRATOR_PASSWORD=")
                .contains("CRYPTO_KEY=");
        for (String linea : ejemplo.lines().filter(linea -> linea.contains("PASSWORD=") || linea.contains("TOKEN=")
                || linea.contains("KEY=")).toList()) {
            assertThat(linea.substring(linea.indexOf('=') + 1)).isEmpty();
        }
    }

    @Test
    void cadaInstanciaUsaSuPropioEnvFile() throws Exception {
        String compose = Files.readString(Path.of("..", "deploy", "docker-compose.yml").toAbsolutePath().normalize());
        assertThat(compose).contains("env_file")
                .contains("${ODONTO_ENV_DIR:-../instances}/demo_uno.env")
                .contains("${ODONTO_ENV_DIR:-../instances}/demo_dos.env");
    }

    @Test
    void noHaySecretosEnScriptsAlertasNiWorkflow() throws Exception {
        List<Path> archivos = List.of(
                Path.of("..", "deploy", "bin").toAbsolutePath().normalize().resolve("backup-diario.sh"),
                Path.of("..", "deploy", "bin").toAbsolutePath().normalize().resolve("rollback.sh"),
                Path.of("..", "..", ".github", "workflows", "global-sdd-ci.yml").toAbsolutePath().normalize());
        for (Path archivo : archivos) {
            if (Files.exists(archivo)) {
                String contenido = Files.readString(archivo);
                assertThat(contenido).as("%s", archivo)
                        .doesNotContain("EAA")
                        .doesNotContain("BEGIN PRIVATE KEY")
                        .doesNotContain("password=");
            }
        }
        assertThat(RegistroAlertas.alertar(TipoAlerta.APP_DOWN).mensaje()).doesNotContain("EAA");
    }
}
