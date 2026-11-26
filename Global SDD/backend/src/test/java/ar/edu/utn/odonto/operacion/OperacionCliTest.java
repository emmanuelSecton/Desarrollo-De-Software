package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

class OperacionCliTest {

    @TempDir
    Path temporal;

    @Test
    void elCliDeRetencionAplicaLaPoliticaYRechazaArgumentosInvalidos() throws Exception {
        Path viejo = Files.writeString(temporal.resolve("consultorio_uno-2026-10-20T000000Z.dump"), "viejo");
        Path reciente = Files.writeString(temporal.resolve("consultorio_uno-2026-11-01T000000Z.dump"), "reciente");

        assertThat(RetencionBackups.ejecutar(new String[] {
                temporal.toString(), "2026-11-25T02:00:00Z"})).isZero();
        assertThat(viejo).doesNotExist();
        assertThat(reciente).exists();
        assertThat(RetencionBackups.ejecutar(new String[] {"solo-un-argumento"})).isEqualTo(1);
    }

    @Test
    void elCliDeBackupsFallidosDisparaLaAlerta() {
        List<String> alertas = new ArrayList<>();
        RegistroAlertas.registrar(alerta -> alertas.add(alerta.mensaje()));
        try {
            assertThat(BackupsDiarios.ejecutar(new String[0])).isEqualTo(1);
            assertThat(BackupsDiarios.ejecutar(new String[] {"consultorio_uno"})).isEqualTo(1);
            assertThat(alertas).anyMatch(mensaje -> mensaje.contains(TipoAlerta.BACKUP_FALLIDO.name()));
        } finally {
            RegistroAlertas.registrar(null);
        }
    }

    @Test
    void elCliDeMigracionSeDetieneAnteElPrimerFallo() {
        assertThat(MigradorTodas.ejecutar(new String[] {"uno", "dos"}, slug -> true)).isZero();
        assertThat(MigradorTodas.ejecutar(new String[] {"uno", "dos"}, slug -> !slug.equals("dos")))
                .isEqualTo(1);
        assertThat(MigradorTodas.ejecutar(new String[0], slug -> true)).isEqualTo(1);
    }

    @Test
    void elCliDeRollbackValidaDumpEImagenAntesDeRestaurar() {
        RollbackDespliegue.Port port = new RollbackDespliegue.Port() {
            @Override
            public void dump(String base, String archivo) {}

            @Override
            public void imagen(String slug, String tag) {}
        };
        RestauracionConsultorio.Restaurador restaurador = new RestauracionConsultorio.Restaurador() {
            @Override
            public void restaurar(String base, String archivo) {}

            @Override
            public void reaplicarGrants(String base, String slug) {}
        };
        assertThat(RollbackDespliegue.ejecutar(new String[0], port, restaurador)).isEqualTo(1);
        assertThat(RollbackDespliegue.ejecutar(new String[] {"uno"}, port, restaurador)).isEqualTo(1);
    }

    @Test
    void elCliDeRolloutValidaLosSlugsYDelegueEnElPuerto() {
        RolloutCanario.Port port = new RolloutCanario.Port() {
            @Override
            public void desplegar(String slug) {}

            @Override
            public boolean salud(String slug) {
                return true;
            }

            @Override
            public void migrar(String slug) {}
        };
        assertThat(RolloutCanario.ejecutar(new String[] {"demo_uno  demo_dos"}, port)).isZero();
        assertThat(RolloutCanario.ejecutar(new String[0], port)).isEqualTo(1);
    }

    @Test
    void laPoliticaDeBackupsCubreFalloYArchivoInexistente() {
        assertThat(PoliticaBackups.debeConservar("sin-fecha.dump", Instant.now())).isFalse();
        assertThat(PoliticaBackups.verificar(0).correcto()).isTrue();
        assertThatThrownBy(() -> PoliticaBackups.aplicarRetencion(temporal.resolve("no-existe"), Instant.now()))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void laRestauracionValidaSlugYFalloDeEjecucion() {
        RestauracionConsultorio.Restaurador ok = new RestauracionConsultorio.Restaurador() {
            @Override
            public void restaurar(String base, String archivo) {}

            @Override
            public void reaplicarGrants(String base, String slug) {}
        };
        RestauracionConsultorio.Restaurador roto = new RestauracionConsultorio.Restaurador() {
            @Override
            public void restaurar(String base, String archivo) {
                throw new IllegalStateException("pg_restore fallo");
            }

            @Override
            public void reaplicarGrants(String base, String slug) {}
        };
        assertThat(RestauracionConsultorio.ejecutar("slug-invalido", "archivo", ok).correcto()).isFalse();
        assertThat(RestauracionConsultorio.ejecutar("uno", "archivo", roto).mensaje()).contains("pg_restore fallo");
    }

    @Test
    void elRollbackValidaImagenYFalloDePuerto() {
        RollbackDespliegue.Port port = new RollbackDespliegue.Port() {
            @Override
            public void dump(String base, String archivo) {}

            @Override
            public void imagen(String slug, String tag) {}
        };
        RollbackDespliegue.Port roto = new RollbackDespliegue.Port() {
            @Override
            public void dump(String base, String archivo) {
                throw new IllegalStateException("pg_dump fallo");
            }

            @Override
            public void imagen(String slug, String tag) {}
        };
        RestauracionConsultorio.Restaurador restaurador = new RestauracionConsultorio.Restaurador() {
            @Override
            public void restaurar(String base, String archivo) {}

            @Override
            public void reaplicarGrants(String base, String slug) {}
        };
        assertThat(RollbackDespliegue.ejecutar("uno", "archivo", "latest", port, restaurador).correcto())
                .isFalse();
        assertThat(RollbackDespliegue.ejecutar("uno", "archivo", "odonto:1.0.0", roto, restaurador).mensaje())
                .contains("pg_dump fallo");
    }

    @Test
    void elBloqueCaddyEsIdempotenteYLaSaludSeLeeDelServidor() throws Exception {
        Path caddy = temporal.resolve("Caddyfile");
        AltaConsultorio.PlanAlta plan = AltaConsultorio.planificar("consultorio_uno", "demo.test", temporal,
                java.util.Map.of());
        AltaConsultorio.agregarBloqueCaddy(caddy, plan);
        String primero = Files.readString(caddy);
        AltaConsultorio.agregarBloqueCaddy(caddy, plan);
        assertThat(Files.readString(caddy)).isEqualTo(primero);
        assertThat(AltaConsultorio.literal("O'Brien")).isEqualTo("'O''Brien'");

        HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/health", intercambio -> {
            byte[] cuerpo = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(200, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
        try {
            assertThat(AltaConsultorio.verificarSalud("http://localhost:" + servidor.getAddress().getPort() + "/health"))
                    .contains("UP");
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void elIndicadorDeSaludAlertaCuandoNoHayDataSource() {
        ObjectProvider<DataSource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        List<String> alertas = new ArrayList<>();
        RegistroAlertas.registrar(alerta -> alertas.add(alerta.mensaje()));
        try {
            assertThat(new AlertaSaludBaseIndicator(provider).health().getStatus().getCode()).isEqualTo("DOWN");
            assertThat(alertas).anyMatch(mensaje -> mensaje.contains(TipoAlerta.BASE_DOWN.name()));
        } finally {
            RegistroAlertas.registrar(null);
        }
    }

    @Test
    void elRetencionCliRechazaFechasInvalidas() {
        assertThatThrownBy(() -> RetencionBackups.ejecutar(
                new String[] {temporal.toString(), "no-es-una-fecha"}))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    void losAdaptadoresDockerConstruyenLosComandosSinEjecutarlos() {
        List<String> comandos = new ArrayList<>();
        ProcesadorComandos procesador = (constructor, salida) -> {
            comandos.add(String.join(" ", constructor.command()));
            return "true";
        };

        AltaConsultorio.Port alta = AltaConsultorio.dockerPort("odonto", procesador);
        alta.sql("select 1");
        alta.sqlEnBase("consultorio_uno", "select 2");
        assertThat(alta.consultaBooleana("select true")).isTrue();

        RollbackDespliegue.Port rollback = RollbackDespliegue.dockerPort("/tmp/backup.dump", "odonto", procesador);
        rollback.dump("consultorio_uno", "/tmp/backup.dump");
        rollback.imagen("uno", "odonto:1.0.0");
        RestauracionConsultorio.Restaurador restaurador =
                RollbackDespliegue.dockerRestaurador("odonto", procesador);
        restaurador.restaurar("consultorio_uno", "/tmp/backup.dump");
        restaurador.reaplicarGrants("consultorio_uno", "uno");

        RolloutCanario.Port rollout = RolloutCanario.dockerPort("http://localhost", procesador);
        rollout.desplegar("uno");
        rollout.migrar("uno");

        assertThat(comandos).anyMatch(comando -> comando.contains("pg_dump"));
        assertThat(comandos).anyMatch(comando -> comando.contains("pg_restore"));
        assertThat(comandos).anyMatch(comando -> comando.contains("migrar-base.sh"));
        assertThat(comandos).anyMatch(comando -> comando.contains("up -d --no-build"));
    }
}
