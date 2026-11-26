package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.StandardEnvironment;

class OperacionRuntimeCableadoTest {

    @Test
    void lasOperacionesRealesDisparanSusAlertas() {
        List<TipoAlerta> capturadas = new ArrayList<>();
        RegistroAlertas.registrar(alerta -> capturadas.add(TipoAlerta.valueOf(alerta.mensaje()
                .substring(alerta.mensaje().lastIndexOf(' ') + 1))));
        try {
            RolloutCanario.ejecutar(List.of("demo_uno", "demo_dos", "otro"), new RolloutCanario.Port() {
                @Override
                public void desplegar(String slug) {}

                @Override
                public boolean salud(String slug) {
                    return !slug.equals("demo_dos");
                }

                @Override
                public void migrar(String slug) {}
            });
            MigradorInstancias.ejecutar(List.of("uno"), slug -> false);
            PoliticaBackups.verificar(1);

            assertThat(capturadas).contains(TipoAlerta.APP_DOWN, TipoAlerta.MIGRACION_FALLIDA,
                    TipoAlerta.BACKUP_FALLIDO);
        } finally {
            RegistroAlertas.registrar(null);
        }
    }

    @Test
    void elEncoderJsonRealDescartaMdcNoPermitido() {
        LoggerContext contexto = (LoggerContext) LoggerFactory.getILoggerFactory();
        FiltroMdcTurboFilter filtro = new FiltroMdcTurboFilter();
        filtro.setContext(contexto);
        filtro.start();
        contexto.addTurboFilter(filtro);
        Logger logger = contexto.getLogger("operacion.cableado");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(contexto);
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
        contexto.putObject(org.springframework.core.env.Environment.class.getName(), new StandardEnvironment());
        StructuredLogEncoder encoder = new StructuredLogEncoder();
        encoder.setContext(contexto);
        encoder.setFormat("logstash");
        encoder.start();
        try {
            MDC.put("request_id", "r-1");
            MDC.put("consultorio_slug", "demo_uno");
            MDC.put("telefono", "123456789");
            MDC.put("detalle_clinico", "diagnostico");
            logger.info("evento de prueba");

            assertThat(appender.list).hasSize(1);
            String json = new String(encoder.encode(appender.list.get(0)), StandardCharsets.UTF_8);
            assertThat(json).contains("r-1")
                    .doesNotContain("123456789")
                    .doesNotContain("diagnostico");
        } finally {
            MDC.clear();
            logger.detachAppender(appender);
            contexto.getTurboFilterList().remove(filtro);
            encoder.stop();
        }
    }

    @Test
    void losScriptsDeOperacionDeleguenEnLaLogicaCableada() throws Exception {
        assertThat(script("migrar-todas.sh")).contains("MigradorTodas");
        assertThat(script("rollout-canario.sh")).contains("RolloutCanario");
        assertThat(script("backup-diario.sh")).contains("RetencionBackups");
        assertThat(script("rollback.sh")).contains("RollbackDespliegue");
    }

    @Test
    void laReferenciaDeImagenSeValidaAntesDeDesplegar() throws Exception {
        String alta = Files.readString(Path.of("src/main/java/ar/edu/utn/odonto/operacion/AltaConsultorio.java"));
        String rollout = Files.readString(Path.of("src/main/java/ar/edu/utn/odonto/operacion/RolloutCanario.java"));
        String rollback = Files.readString(Path.of("src/main/java/ar/edu/utn/odonto/operacion/RollbackDespliegue.java"));
        assertThat(alta + rollout + rollback).contains("ReferenciaImagen.esValida");
    }

    private static String script(String nombre) throws Exception {
        return Files.readString(Path.of("..", "deploy", "bin", nombre).toAbsolutePath().normalize());
    }
}
