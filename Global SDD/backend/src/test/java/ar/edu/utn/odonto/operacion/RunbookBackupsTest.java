package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RunbookBackupsTest {

    @Test
    void documentaBackupRestorePruebaTrimestralYFicticios() throws Exception {
        String runbook = Files.readString(Path.of("..", "deploy", "RUNBOOK-operacion.md")
                .toAbsolutePath()
                .normalize());
        assertThat(runbook).contains("backup-diario.sh")
                .contains("restaurar-consultorio.sh")
                .contains("prueba trimestral")
                .contains("roles y grants")
                .contains("SU-07")
                .contains("PA-13")
                .contains("PA-09");
    }

    @Test
    void noContieneSecretosReales() throws Exception {
        String runbook = Files.readString(Path.of("..", "deploy", "RUNBOOK-operacion.md")
                .toAbsolutePath()
                .normalize());
        assertThat(runbook).doesNotContain("password=")
                .doesNotContain("BEGIN PRIVATE KEY")
                .doesNotContain("EAA")
                .doesNotContain("wa-token-");
    }

    @Test
    void losComandosDocumentadosCoincidenConLosScripts() throws Exception {
        String runbook = Files.readString(Path.of("..", "deploy", "RUNBOOK-operacion.md")
                .toAbsolutePath()
                .normalize());
        assertThat(runbook).contains("deploy/bin/backup-diario.sh")
                .contains("deploy/bin/restaurar-consultorio.sh")
                .contains("<slug>");
    }
}
