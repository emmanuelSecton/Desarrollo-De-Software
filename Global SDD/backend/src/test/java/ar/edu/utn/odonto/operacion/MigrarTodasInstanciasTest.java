package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class MigrarTodasInstanciasTest {

    private static final String CLAVE = "clave-ficticia-de-migracion-todas";

    private static PostgreSQLContainer<?> contenedor;

    @BeforeAll
    static void iniciar() throws Exception {
        contenedor = new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", "todas_uno todas_dos")
                .withEnv("CONSULTORIO_TODAS_UNO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_TODAS_UNO_RUNTIME_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_TODAS_DOS_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_TODAS_DOS_RUNTIME_PASSWORD", CLAVE)
                .withCopyFileToContainer(MountableFile.forHostPath(
                                java.nio.file.Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                                        .toAbsolutePath()
                                        .normalize(),
                                0755),
                        "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
        contenedor.start();
    }

    @AfterAll
    static void detener() {
        if (contenedor != null) {
            contenedor.stop();
        }
    }

    @Test
    void todasLasBasesQuedanConElMismoHistorialYVersion() throws Exception {
        List<String> historialUno = migrarYLeer("todas_uno");
        List<String> historialDos = migrarYLeer("todas_dos");

        assertThat(historialUno).containsExactly("1", "2");
        assertThat(historialDos).isEqualTo(historialUno);
    }

    @Test
    void unFalloEnUnaBaseDetieneElProcesoYNoArrancaLasDemas() {
        List<String> intentadas = new ArrayList<>();
        MigradorInstancias.Resultado resultado = MigradorInstancias.ejecutar(
                List.of("uno", "dos", "tres"), slug -> {
                    intentadas.add(slug);
                    return !slug.equals("dos");
                });

        assertThat(resultado.correcto()).isFalse();
        assertThat(resultado.fallida()).isEqualTo("dos");
        assertThat(intentadas).containsExactly("uno", "dos");
    }

    @Test
    void cuandoTodasMigranCorrectamenteSeRegistranEnOrden() {
        MigradorInstancias.Resultado resultado =
                MigradorInstancias.ejecutar(List.of("uno", "dos"), slug -> true);
        assertThat(resultado.correcto()).isTrue();
        assertThat(resultado.migradas()).containsExactly("uno", "dos");
        assertThat(resultado.fallida()).isNull();
    }

    @Test
    void existeUnScriptQueMigraTodasLasInstanciasYSeDetieneAnteElPrimerFallo() throws Exception {
        String contenido = java.nio.file.Files.readString(
                java.nio.file.Path.of("..", "deploy", "bin", "migrar-todas.sh").toAbsolutePath().normalize());
        assertThat(contenido).contains("migrar-base.sh")
                .contains("set -euo pipefail")
                .contains("CONSULTORIO_SLUGS");
    }

    private static List<String> migrarYLeer(String slug) throws Exception {
        MigracionesC35.aplicar(contenedor, slug, CLAVE);
        try (Connection conexion = DriverManager.getConnection(
                        MigracionesC35.urlBase(contenedor, slug), contenedor.getUsername(), contenedor.getPassword());
                Statement sentencia = conexion.createStatement()) {
            return MigracionesC35.consultaLista(sentencia,
                    "select version from flyway_schema_history where success order by installed_rank");
        }
    }
}
