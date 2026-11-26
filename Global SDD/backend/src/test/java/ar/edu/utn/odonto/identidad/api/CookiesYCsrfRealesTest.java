package ar.edu.utn.odonto.identidad.api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Flujo real de la SPA sobre un servidor HTTP (DD-08, D3, D7): token CSRF por cookie reenviado como
 * cabecera, y atributos de la cookie de sesion que aplica el contenedor (MockMvc no los aplica).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class CookiesYCsrfRealesTest {

    @LocalServerPort
    int puerto;

    @Autowired
    DataSource dataSource;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @Test
    void laSpaObtieneElTokenCsrfIniciaSesionYRecibeUnaCookieDeSesionSegura() throws Exception {
        UsuariosDePrueba.Creado secretaria = UsuariosDePrueba.secretaria(new JdbcTemplate(dataSource));

        HttpResponse<String> csrf = cliente.send(
                HttpRequest.newBuilder(url("/api/v1/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(csrf.statusCode()).isEqualTo(200);
        assertThat(csrf.body()).contains("\"cabecera\":\"X-XSRF-TOKEN\"");
        String cookieCsrf = cookie(csrf, "XSRF-TOKEN").orElseThrow();
        assertThat(cookieCsrf).contains("Secure").contains("SameSite=Strict").doesNotContain("HttpOnly");
        String token = valor(cookieCsrf);

        HttpResponse<String> sinCabecera = login(secretaria, "XSRF-TOKEN=" + token, null);
        assertThat(sinCabecera.statusCode()).isEqualTo(403);

        HttpResponse<String> login = login(secretaria, "XSRF-TOKEN=" + token, token);
        assertThat(login.statusCode()).isEqualTo(200);
        String cookieSesion = cookie(login, "JSESSIONID").orElseThrow();
        assertThat(cookieSesion).contains("HttpOnly").contains("Secure").contains("SameSite=Strict");

        HttpResponse<String> me = cliente.send(HttpRequest.newBuilder(url("/api/v1/auth/me"))
                        .header("Cookie", "JSESSIONID=" + valor(cookieSesion))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(secretaria.id().toString());
    }

    private HttpResponse<String> login(UsuariosDePrueba.Creado usuario, String cookies, String cabecera)
            throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(url("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("Cookie", cookies)
                .POST(HttpRequest.BodyPublishers.ofString(
                        UsuariosDePrueba.cuerpoLogin(usuario.email(), usuario.clave())));
        if (cabecera != null) {
            peticion.header("X-XSRF-TOKEN", cabecera);
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }

    private static Optional<String> cookie(HttpResponse<?> respuesta, String nombre) {
        List<String> cookies = respuesta.headers().allValues("Set-Cookie");
        return cookies.stream().filter(c -> c.startsWith(nombre + "=")).findFirst();
    }

    private static String valor(String setCookie) {
        String par = setCookie.split(";", 2)[0];
        return par.substring(par.indexOf('=') + 1);
    }
}
