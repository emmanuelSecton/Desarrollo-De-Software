package ar.edu.utn.odonto.identidad.infrastructure;

import ar.edu.utn.odonto.identidad.application.Problemas;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite de peticiones por IP para {@code POST /auth/login} y {@code /public/**} (D10).
 * Corre antes que la cadena de seguridad: un 429 no evalua credenciales. La IP es la del socket;
 * confiar en {@code X-Forwarded-For} queda para C-35.
 */
public class FiltroLimiteDePeticiones extends OncePerRequestFilter {

    static final String TITULO = "Demasiadas peticiones";
    static final String DETALLE = "Se superó el límite de peticiones; intente de nuevo más tarde";
    private static final Duration PERIODO = Duration.ofMinutes(1);

    private final EscritorDeProblemas escritor;
    private final long loginPorMinuto;
    private final long publicoPorMinuto;
    /** Acotado y con expiracion: IPs nuevas no hacen crecer la memoria sin limite. */
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public FiltroLimiteDePeticiones(EscritorDeProblemas escritor, long loginPorMinuto, long publicoPorMinuto) {
        this.escritor = escritor;
        this.loginPorMinuto = loginPorMinuto;
        this.publicoPorMinuto = publicoPorMinuto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !RutasDeSeguridad.esAutenticacion(peticion) && !RutasDeSeguridad.esPublica(peticion);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        boolean login = RutasDeSeguridad.esAutenticacion(peticion);
        long cupo = login ? loginPorMinuto : publicoPorMinuto;
        String clave = (login ? "login:" : "publico:") + peticion.getRemoteAddr();
        Bucket bucket = buckets.get(clave, k -> nuevoBucket(cupo));

        ConsumptionProbe intento = bucket.tryConsumeAndReturnRemaining(1);
        if (intento.isConsumed()) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        long segundos = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(intento.getNanosToWaitForRefill() + 999_999_999L));
        respuesta.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(segundos));
        escritor.escribir(peticion, respuesta, HttpStatus.TOO_MANY_REQUESTS,
                Problemas.DEMASIADAS_PETICIONES, TITULO, DETALLE);
    }

    private static Bucket nuevoBucket(long cupo) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(cupo).refillGreedy(cupo, PERIODO).build())
                .build();
    }
}
