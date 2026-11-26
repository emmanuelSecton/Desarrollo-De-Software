package ar.edu.utn.odonto.operacion;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.Map;
import org.slf4j.MDC;
import org.slf4j.Marker;

public final class FiltroMdcTurboFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format,
            Object[] parameters, Throwable throwable) {
        if (!(MDC.getMDCAdapter() instanceof LogbackMDCAdapter adaptador)) {
            return FilterReply.NEUTRAL;
        }
        Map<String, String> valores = adaptador.getPropertyMap();
        if (valores != null) {
            for (String clave : valores.keySet().toArray(String[]::new)) {
                if (!FiltroMdcLogs.permitido(clave)) {
                    MDC.remove(clave);
                }
            }
        }
        return FilterReply.NEUTRAL;
    }
}
