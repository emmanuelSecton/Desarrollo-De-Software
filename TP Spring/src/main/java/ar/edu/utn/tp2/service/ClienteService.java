package ar.edu.utn.tp2.service;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.tp2.Cliente;
import ar.edu.utn.tp2.Contacto;
import ar.edu.utn.tp2.Domicilio;
import ar.edu.utn.tp2.Usuario;
import ar.edu.utn.tp2.dto.ClienteDTO;
import ar.edu.utn.tp2.dto.ClienteRequestDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ClienteService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public ClienteDTO crear(ClienteRequestDTO dto) {
        String denominacion = obligatorio(dto.getDenominacion(), "La denominación / razón social");
        String cuit = normalizarCuit(obligatorio(dto.getCuitCuil(), "El CUIT/CUIL"));
        String nombreCalle = obligatorio(dto.getNombreCalle(), "La calle del domicilio");
        String numeroCalle = obligatorio(dto.getNumeroCalle(), "El número del domicilio");
        String email = opcional(dto.getEmail());

        if (email != null && !EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email no tiene un formato válido.");
        }

        Long existentes = em.createQuery(
                        "SELECT COUNT(c) FROM Cliente c WHERE REPLACE(c.cuitCuil, '-', '') = :digitos", Long.class)
                .setParameter("digitos", cuit.replace("-", ""))
                .getSingleResult();
        if (existentes > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con el CUIT/CUIL " + cuit + ".");
        }

        Usuario usuario = obtenerUsuarioPorDefecto();
        Date ahora = new Date();

        Contacto contacto = new Contacto(email, opcional(dto.getTelefono()), opcional(dto.getCelular()));
        Domicilio domicilio = new Domicilio(nombreCalle, numeroCalle);
        em.persist(contacto);
        em.persist(domicilio);

        Cliente cliente = new Cliente(cuit, denominacion, contacto, domicilio);
        cliente.setFechaAlta(ahora);
        cliente.setFechaModificacion(ahora);
        cliente.setUsuarioCarga(usuario);
        cliente.setUsuarioModificac(usuario);
        em.persist(cliente);
        em.flush();

        return new ClienteDTO(cliente.getId(), cliente.getDenominacion(), cliente.getCuitCuil());
    }

    /** Acepta "20123456789" o "20-12345678-9" y devuelve siempre el formato XX-XXXXXXXX-X. */
    private String normalizarCuit(String valor) {
        String digitos = valor.replaceAll("[^0-9]", "");
        if (digitos.length() != 11) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El CUIT/CUIL debe tener 11 dígitos.");
        }
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }

    private String obligatorio(String valor, String campo) {
        String limpio = opcional(valor);
        if (limpio == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " es obligatorio.");
        }
        return limpio;
    }

    private String opcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private Usuario obtenerUsuarioPorDefecto() {
        List<Usuario> usuarios = em.createQuery("SELECT u FROM Usuario u ORDER BY u.id", Usuario.class)
                .setMaxResults(1)
                .getResultList();
        if (usuarios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No hay usuarios registrados para la auditoría.");
        }
        return usuarios.get(0);
    }
}
