package ar.edu.utn.odonto.identidad.api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.support.MensajeriaDePrueba;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Restablecer la contraseña cierra las sesiones abiertas del usuario, sobre un servidor HTTP real (C-04, D7). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({PostgresTestConfig.class, MensajeriaDePrueba.class})
class CierreDeSesionesTest {

    @LocalServerPort
    int puerto;

    @Autowired
    DataSource dataSource;

    @Autowired
    MensajeriaDePrueba.Capturados correo;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @Test
    void restablecerCierraTodasLasSesionesDelUsuarioYNoLasDeOtros() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UsuariosDePrueba.Creado usuario = UsuariosDePrueba.secretaria(jdbc);
        UsuariosDePrueba.Creado otro = UsuariosDePrueba.secretaria(jdbc);
        String token = csrf();
        String sesion1 = login(usuario, token);
        String sesion2 = login(usuario, token);
        String sesionOtro = login(otro, token);
        assertThat(me(sesion1)).isEqualTo(200);
        assertThat(me(sesion2)).isEqualTo(200);

        enviar("/api/v1/auth/password/recover", "{\"email\":\"" + usuario.email() + "\"}", token, null);
        String enlace = MensajeriaDePrueba.Capturados.token(
                correo.esperar(usuario.email(), Duration.ofSeconds(5)).orElseThrow());
        HttpResponse<String> reset = enviar("/api/v1/auth/password/reset",
                "{\"token\":\"" + enlace + "\",\"nueva\":\"una-clave-nueva-segura\"}", token, null);

        assertThat(reset.statusCode()).isEqualTo(204);
        assertThat(me(sesion1)).isEqualTo(401);
        assertThat(me(sesion2)).isEqualTo(401);
        assertThat(me(sesionOtro)).isEqualTo(200);
    }

    private String csrf() throws Exception {
        HttpResponse<String> respuesta = cliente.send(HttpRequest.newBuilder(url("/api/v1/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return valorCookie(respuesta, "XSRF-TOKEN");
    }

    private String login(UsuariosDePrueba.Creado usuario, String token) throws Exception {
        HttpResponse<String> respuesta = enviar("/api/v1/auth/login",
                UsuariosDePrueba.cuerpoLogin(usuario.email(), usuario.clave()), token, null);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return valorCookie(respuesta, "JSESSIONID");
    }

    private int me(String sesion) throws Exception {
        return cliente.send(HttpRequest.newBuilder(url("/api/v1/auth/me"))
                        .header("Cookie", "JSESSIONID=" + sesion).GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private HttpResponse<String> enviar(String ruta, String cuerpo, String token, String sesion) throws Exception {
        String cookies = "XSRF-TOKEN=" + token + (sesion == null ? "" : "; JSESSIONID=" + sesion);
        return cliente.send(HttpRequest.newBuilder(url(ruta))
                        .header("Content-Type", "application/json")
                        .header("Cookie", cookies)
                        .header("X-XSRF-TOKEN", token)
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }

    private static String valorCookie(HttpResponse<?> respuesta, String nombre) {
        String setCookie = respuesta.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith(nombre + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta la cookie " + nombre));
        String par = setCookie.split(";", 2)[0];
        return par.substring(par.indexOf('=') + 1);
    }
}
