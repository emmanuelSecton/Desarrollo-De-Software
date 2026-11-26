package ar.edu.utn.odonto.identidad.infrastructure;

import ar.edu.utn.odonto.identidad.application.AltaOdontologoInicial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al arrancar, crea el odontologo inicial si la instancia no tiene ninguno (D2). */
@Component
public class InicializadorOdontologo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorOdontologo.class);

    private final AltaOdontologoInicial alta;
    private final PropiedadesIdentidad propiedades;

    public InicializadorOdontologo(AltaOdontologoInicial alta, PropiedadesIdentidad propiedades) {
        this.alta = alta;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (alta.crearSiFalta(propiedades.emailInicial(), propiedades.claveInicial())) {
            // Sin email ni contraseña en el log (Ley 25.326).
            LOG.info("Se creó el odontólogo inicial; debe cambiar la contraseña en su primer login");
        }
    }
}
