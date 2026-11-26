package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackupDiarioTest {

    private static final Instant HOY = Instant.parse("2026-11-25T02:00:00Z");

    @Test
    void conservaBackupsDeLosUltimosTreintaDias() {
        assertThat(PoliticaBackups.debeConservar("consultorio_demo_uno-2026-11-01T000000Z.dump", HOY))
                .isTrue();
        assertThat(PoliticaBackups.debeConservar("consultorio_demo_uno-2026-10-27T000000Z.dump", HOY))
                .isTrue();
        assertThat(PoliticaBackups.debeConservar("consultorio_demo_uno-2026-10-20T000000Z.dump", HOY))
                .isFalse();
    }

    @Test
    void unFallóDelDumpSeReportaExplicitamente() {
        assertThat(PoliticaBackups.verificar(0).correcto()).isTrue();
        PoliticaBackups.Resultado fallo = PoliticaBackups.verificar(1);
        assertThat(fallo.correcto()).isFalse();
        assertThat(fallo.mensaje()).contains("fallo");
    }

    @Test
    void elJobGeneraElMismoFormatoQueLeeLaPolitica() throws Exception {
        assertThat(PoliticaBackups.debeConservar("consultorio_demo_uno-2026-11-01T000000Z.dump", HOY))
                .isTrue();
        assertThat(PoliticaBackups.debeConservar("consultorio_demo_uno-2026-10-20T000000Z.dump", HOY))
                .isFalse();
        String script = script();
        assertThat(script).contains("+%Y-%m-%dT%H%M%SZ")
                .doesNotContain("+%Y%m%dT%H%M%SZ");
    }

    @Test
    void laRetencionRealBorraSoloLosBackupsVencidosDelDirectorio(@TempDir Path directorio) throws Exception {
        Path viejo = directorio.resolve("consultorio_demo_uno-2026-10-20T000000Z.dump");
        Path reciente = directorio.resolve("consultorio_demo_uno-2026-11-01T000000Z.dump");
        Path ajeno = directorio.resolve("NOTAS.txt");
        Files.writeString(viejo, "viejo");
        Files.writeString(reciente, "reciente");
        Files.writeString(ajeno, "no tocar");

        PoliticaBackups.aplicarRetencion(directorio, HOY);

        try (Stream<Path> restantes = Files.list(directorio)) {
            assertThat(restantes.map(Path::getFileName).map(Object::toString))
                    .containsExactlyInAnyOrder("consultorio_demo_uno-2026-11-01T000000Z.dump", "NOTAS.txt");
        }
    }

    @Test
    void elJobInvocaElHelperDeRetencionEnLugarDeUnFindOpaco() throws Exception {
        assertThat(script()).contains("RetencionBackups", "ODONTO_JAR")
                .doesNotContain("-mtime +30");
    }

    @Test
    void elJobHaceUnDumpPorBaseConRetencionYAlerta() throws Exception {
        assertThat(script()).contains("pg_dump")
                .contains("CONSULTORIO_SLUGS")
                .contains("ODONTO_BACKUP_DIR")
                .contains("set -euo pipefail")
                .contains("exit 1");
    }

    private static String script() throws Exception {
        return Files.readString(Path.of("..", "deploy", "bin", "backup-diario.sh")
                .toAbsolutePath()
                .normalize());
    }
}
