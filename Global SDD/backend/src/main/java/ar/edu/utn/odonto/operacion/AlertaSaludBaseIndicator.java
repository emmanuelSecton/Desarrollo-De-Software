package ar.edu.utn.odonto.operacion;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public final class AlertaSaludBaseIndicator implements HealthIndicator {

    private final ObjectProvider<DataSource> dataSource;

    public AlertaSaludBaseIndicator(ObjectProvider<DataSource> dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        DataSource fuente = dataSource.getIfAvailable();
        if (fuente == null) {
            RegistroAlertas.alertar(TipoAlerta.BASE_DOWN);
            return Health.down().build();
        }
        try (Connection conexion = fuente.getConnection(); Statement sentencia = conexion.createStatement()) {
            sentencia.execute("select 1");
            return Health.up().build();
        } catch (Exception fallo) {
            RegistroAlertas.alertar(TipoAlerta.BASE_DOWN);
            return Health.down().build();
        }
    }
}
