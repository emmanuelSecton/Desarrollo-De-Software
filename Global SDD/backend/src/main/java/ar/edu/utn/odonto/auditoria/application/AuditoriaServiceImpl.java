package ar.edu.utn.odonto.auditoria.application;

import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Registro validado y delegado al puerto en su propia transacción (D3/D4). */
@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    static final int MAX_ACCION = 60;
    static final int MAX_ACTOR_TIPO = 20;
    static final int MAX_ENTIDAD = 80;
    static final int MAX_ENTIDAD_ID = 100;

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
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(RegistroAuditoria registro) {
        Objects.requireNonNull(registro, "registro");
        validarCampos(registro);
        Map<String, Object> detalle = validadorDetalle.validar(registro.detalle());
        String ip = validadorIp.validar(registro.ip());
        puerto.guardar(new RegistroAuditoria(registro.accion(), registro.tipoActor(), registro.actorId(),
                registro.entidad(), registro.entidadId(), detalle, ip));
    }

    static void validarCampos(RegistroAuditoria registro) {
        validarTexto("accion", registro.accion() == null ? null : registro.accion().valor(), MAX_ACCION);
        validarTexto("actor_tipo", registro.tipoActor() == null ? null : registro.tipoActor().valor(),
                MAX_ACTOR_TIPO);
        validarTexto("entidad", registro.entidad(), MAX_ENTIDAD);
        validarTexto("entidad_id", registro.entidadId(), MAX_ENTIDAD_ID);
    }

    static void validarTexto(String campo, String valor, int maximo) {
        if (valor == null || valor.length() > maximo) {
            throw new AuditoriaInvalidaException(campo + ": valor invalido");
        }
    }
}
