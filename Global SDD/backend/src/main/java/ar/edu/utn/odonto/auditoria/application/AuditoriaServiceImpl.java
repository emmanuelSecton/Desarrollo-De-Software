package ar.edu.utn.odonto.auditoria.application;

import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Registro validado y delegado al puerto en su propia transacción (D3/D4). */
@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private final EventoAuditoriaPort puerto;
    private final ValidadorDetalle validadorDetalle;
    private final ValidadorIp validadorIp;

    public AuditoriaServiceImpl(
            EventoAuditoriaPort puerto, ValidadorDetalle validadorDetalle, ValidadorIp validadorIp) {
        this.puerto = puerto;
        this.validadorDetalle = validadorDetalle;
        this.validadorIp = validadorIp;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(RegistroAuditoria registro) {
        Objects.requireNonNull(registro, "registro");
        Map<String, Object> detalle = validadorDetalle.validar(registro.detalle());
        String ip = validadorIp.validar(registro.ip());
        puerto.guardar(new RegistroAuditoria(registro.accion(), registro.tipoActor(), registro.actorId(),
                registro.entidad(), registro.entidadId(), detalle, ip));
    }
}
