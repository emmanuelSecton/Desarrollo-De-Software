package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AlertasMinimasTest {

    @Test
    void disparaAlertasParaAppBaseBackupYMigracion() {
        for (TipoAlerta tipo : List.of(
                TipoAlerta.APP_DOWN,
                TipoAlerta.BASE_DOWN,
                TipoAlerta.BACKUP_FALLIDO,
                TipoAlerta.MIGRACION_FALLIDA)) {
            assertThat(RegistroAlertas.alertar(tipo).disparada()).as("%s", tipo).isTrue();
            assertThat(RegistroAlertas.alertar(tipo).mensaje()).as("%s", tipo).contains(tipo.name());
        }
    }

    @Test
    void noAlertaSobreTokenDeMetaPuesCorrespondeAC15C16() {
        assertThat(RegistroAlertas.alertar(TipoAlerta.WA_TOKEN_EXPIRADO).disparada()).isFalse();
        assertThat(RegistroAlertas.alertar(TipoAlerta.WA_TOKEN_EXPIRADO).mensaje()).isEmpty();
    }

    @Test
    void elMensajeNoIncluyeValoresSensibles() {
        assertThat(RegistroAlertas.alertar(TipoAlerta.APP_DOWN).mensaje())
                .doesNotContain("token")
                .doesNotContain("password")
                .doesNotContain("EAA");
    }
}
