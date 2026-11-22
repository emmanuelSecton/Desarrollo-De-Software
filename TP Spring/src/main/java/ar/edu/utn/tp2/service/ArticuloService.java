package ar.edu.utn.tp2.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.tp2.Articulo;
import ar.edu.utn.tp2.AuditoriaApp;
import ar.edu.utn.tp2.ListaPrecio;
import ar.edu.utn.tp2.ListaPrecioArticulo;
import ar.edu.utn.tp2.Marca;
import ar.edu.utn.tp2.Rubro;
import ar.edu.utn.tp2.Usuario;
import ar.edu.utn.tp2.dto.ArticuloRequestDTO;
import ar.edu.utn.tp2.dto.ListaPrecioArticuloDTO;
import ar.edu.utn.tp2.dto.PrecioListaDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ArticuloService {

    @PersistenceContext
    private EntityManager em;

    /** Crea el Articulo y sus precios (uno por lista cargada), en una sola transacción. */
    @Transactional
    public List<ListaPrecioArticuloDTO> crear(ArticuloRequestDTO dto) {
        String codigo = obligatorio(dto.getCodigo(), "El código").toUpperCase();
        String denominacion = obligatorio(dto.getDenominacion(), "La denominación");

        List<PrecioListaDTO> precios = dto.getPrecios() != null ? dto.getPrecios() : List.of();
        if (precios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargá el precio en al menos una lista.");
        }
        Set<Long> listasVistas = new HashSet<>();
        for (PrecioListaDTO p : precios) {
            if (p.getListaPrecioId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada precio debe indicar su lista de precios.");
            }
            if (!listasVistas.add(p.getListaPrecioId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay más de un precio para la misma lista.");
            }
            if (p.getPrecioVenta() == null || p.getPrecioVenta() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los precios deben ser mayores a 0.");
            }
        }

        Long existentes = em.createQuery(
                        "SELECT COUNT(a) FROM Articulo a WHERE UPPER(a.codigo) = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
        if (existentes > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un artículo con el código " + codigo + ".");
        }

        Rubro rubro = dto.getRubroId() != null ? buscar(Rubro.class, dto.getRubroId(), "Rubro") : null;
        Marca marca = dto.getMarcaId() != null ? buscar(Marca.class, dto.getMarcaId(), "Marca") : null;

        Usuario usuario = obtenerUsuarioPorDefecto();

        Articulo articulo = new Articulo(rubro, codigo, denominacion, marca);
        auditar(articulo, usuario);
        em.persist(articulo);

        List<ListaPrecioArticulo> creados = new ArrayList<>();
        for (PrecioListaDTO p : precios) {
            ListaPrecio lista = buscar(ListaPrecio.class, p.getListaPrecioId(), "Lista de precios");
            ListaPrecioArticulo precio = new ListaPrecioArticulo(lista, p.getPrecioVenta(), articulo);
            auditar(precio, usuario);
            em.persist(precio);
            creados.add(precio);
        }
        em.flush();

        return creados.stream()
                .map(precio -> new ListaPrecioArticuloDTO(precio.getId(), articulo.getId(), articulo.getDenominacion(),
                        precio.getPrecioVenta(), precio.getListaPrecio().getId()))
                .toList();
    }

    private <T> T buscar(Class<T> clase, Long id, String nombre) {
        T entidad = em.find(clase, id);
        if (entidad == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, nombre + " no encontrado/a (ID " + id + ").");
        }
        return entidad;
    }

    private void auditar(AuditoriaApp entidad, Usuario usuario) {
        Date ahora = new Date();
        entidad.setFechaAlta(ahora);
        entidad.setFechaModificacion(ahora);
        entidad.setUsuarioCarga(usuario);
        entidad.setUsuarioModificac(usuario);
    }

    private String obligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " es obligatorio.");
        }
        return valor.trim();
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
