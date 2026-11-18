import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

public class AfipWsaaClient {

    private static final String URL_WSAA_HOMO = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String SERVICIO = "wsfe";
    /** Se pide un TA nuevo si al guardado le quedan menos de esto. */
    private static final Duration MARGEN_VENCIMIENTO = Duration.ofMinutes(10);
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private record RespuestaHttp(int status, String cuerpo) {}

    /**
     * Devuelve un TA vigente para wsfe. Reutiliza el guardado en {@code archivoCache}
     * si sigue vigente; si no, pide uno nuevo a WSAA y lo guarda.
     */
    public static TicketAcceso obtenerTicket(String keyPath, String certPath, Path archivoCache)
            throws AfipAuthException {
        X509Certificate cert = cargarCertificado(certPath);
        String certId = cert.getSerialNumber().toString(16);

        Optional<TicketAcceso> guardado = TicketAcceso.cargar(archivoCache);
        if (guardado.isPresent() && guardado.get().esValidoPara(SERVICIO, certId, MARGEN_VENCIMIENTO)) {
            System.out.println("Se reutiliza el TA guardado (vence " + guardado.get().expiracion() + ").");
            return guardado.get();
        }

        PrivateKey privateKey = cargarClavePrivada(keyPath);
        TicketAcceso nuevo = solicitarNuevoTicket(privateKey, cert, certId);
        try {
            nuevo.guardar(archivoCache);
        } catch (IOException e) {
            // El TA es válido igual; solo no se podrá reutilizar en la próxima ejecución.
            System.err.println("Aviso: no se pudo guardar el TA en " + archivoCache + ": " + e.getMessage());
        }
        return nuevo;
    }

    private static TicketAcceso solicitarNuevoTicket(PrivateKey privateKey, X509Certificate cert, String certId)
            throws AfipAuthException {
        String cmsBase64;
        try {
            cmsBase64 = firmarTra(armarTra(), privateKey, cert);
        } catch (Exception e) {
            throw new AfipAuthException("local.firma",
                    "No se pudo firmar el pedido de acceso. ¿La clave privada corresponde al certificado?", e);
        }

        String soapEnvelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:wsaa=\"http://wsaa.view.sua.dvadac.desein.afip.gov\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<wsaa:loginCms>" +
                "<wsaa:in0>" + cmsBase64 + "</wsaa:in0>" +
                "</wsaa:loginCms>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        RespuestaHttp respuesta = enviarSoap(URL_WSAA_HOMO, soapEnvelope);
        return parsearRespuesta(respuesta, certId);
    }

    private static String armarTra() {
        ZonedDateTime now = ZonedDateTime.now();
        String genTime = now.minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String expTime = now.plusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<loginTicketRequest version=\"1.0\">" +
                "<header>" +
                "<uniqueId>" + (System.currentTimeMillis() / 1000) + "</uniqueId>" +
                "<generationTime>" + genTime + "</generationTime>" +
                "<expirationTime>" + expTime + "</expirationTime>" +
                "</header>" +
                "<service>" + SERVICIO + "</service>" +
                "</loginTicketRequest>";
    }

    private static String firmarTra(String traXml, PrivateKey privateKey, X509Certificate cert) throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC").build(privateKey);

        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                .build(sha256Signer, cert));
        gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));

        CMSSignedData signedData = gen.generate(
                new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8)), true);
        return Base64.getEncoder().encodeToString(signedData.getEncoded());
    }

    /**
     * Interpreta la respuesta de loginCms. Si AFIP devolvió un SOAP Fault, lanza una
     * AfipAuthException con su código. Nunca devuelve un token o sign vacío.
     */
    private static TicketAcceso parsearRespuesta(RespuestaHttp respuesta, String certId) throws AfipAuthException {
        Document envelope = parsearXml(respuesta.cuerpo(),
                "WSAA respondió HTTP " + respuesta.status() + " con un contenido que no es XML");

        if (envelope.getElementsByTagNameNS("*", "Fault").getLength() > 0) {
            String faultcode = texto(envelope, "faultcode");
            String faultstring = texto(envelope, "faultstring");
            String codigo = faultcode == null ? "desconocido" : faultcode.substring(faultcode.lastIndexOf(':') + 1);
            throw new AfipAuthException(codigo, explicarError(codigo, faultstring));
        }
        if (respuesta.status() >= 400) {
            throw new AfipAuthException("http." + respuesta.status(),
                    "WSAA respondió HTTP " + respuesta.status() + " sin detalle del error.");
        }

        // loginCmsReturn trae el loginTicketResponse como texto XML escapado.
        String ticketXml = texto(envelope, "loginCmsReturn");
        if (ticketXml == null) {
            throw new AfipAuthException("respuesta.invalida", "La respuesta de WSAA no contiene loginCmsReturn.");
        }
        Document ticket = parsearXml(ticketXml, "El loginCmsReturn de WSAA no es XML válido");

        String token = texto(ticket, "token");
        String sign = texto(ticket, "sign");
        String expiracion = texto(ticket, "expirationTime");
        if (token == null || sign == null || expiracion == null) {
            throw new AfipAuthException("respuesta.invalida",
                    "La respuesta de WSAA no trae token, sign o expirationTime.");
        }
        try {
            return new TicketAcceso(token, sign, OffsetDateTime.parse(expiracion), SERVICIO, certId);
        } catch (DateTimeParseException e) {
            throw new AfipAuthException("respuesta.invalida",
                    "WSAA devolvió una fecha de vencimiento inválida: " + expiracion, e);
        }
    }

    private static String explicarError(String codigo, String detalleAfip) {
        String explicacion = switch (codigo) {
            case "coe.alreadyAuthenticated" ->
                    "AFIP ya emitió un TA vigente para este certificado y servicio, pero no hay uno guardado "
                            + "localmente (¿se borró el archivo o se pidió desde otro programa?). "
                            + "Hay que esperar a que venza (hasta 12 h) o usar otro certificado.";
            case "coe.notAuthorized" ->
                    "El certificado no está autorizado para el servicio " + SERVICIO
                            + ". Hay que asociarlo en el portal de AFIP (WSASS en homologación).";
            case "cms.cert.expired" -> "El certificado está vencido.";
            case "cms.cert.untrusted" -> "AFIP no reconoce el certificado (¿es de otro ambiente?).";
            case "cms.bad", "cms.sign.invalid" -> "La firma del pedido no es válida.";
            case "xml.generationTime.invalid", "xml.expirationTime.invalid" ->
                    "Las fechas del pedido no son aceptadas. Revisá que la hora del equipo esté sincronizada.";
            default -> "WSAA rechazó la autenticación.";
        };
        return explicacion + " [AFIP: " + (detalleAfip == null ? "sin detalle" : detalleAfip) + "]";
    }

    private static Document parsearXml(String xml, String mensajeError) throws AfipAuthException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            // Evita XXE: la respuesta viene de la red.
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder builder = f.newDocumentBuilder();
            builder.setErrorHandler(null); // el error ya se informa con la excepción; evita "[Fatal Error]" en consola
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new AfipAuthException("respuesta.invalida", mensajeError + ".", e);
        }
    }

    /** Texto del primer elemento con ese nombre local (cualquier namespace), o null si falta o está vacío. */
    private static String texto(Document doc, String nombreLocal) {
        NodeList nodos = doc.getElementsByTagNameNS("*", nombreLocal);
        if (nodos.getLength() == 0) {
            return null;
        }
        String valor = nodos.item(0).getTextContent();
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static PrivateKey cargarClavePrivada(String path) throws AfipAuthException {
        try {
            String content = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
            content = content
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(content);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new AfipAuthException("local.clave",
                    "No se pudo leer la clave privada " + new File(path).getAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    private static X509Certificate cargarCertificado(String path) throws AfipAuthException {
        try (FileInputStream fis = new FileInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fis);
        } catch (Exception e) {
            throw new AfipAuthException("local.certificado",
                    "No se pudo leer el certificado " + new File(path).getAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    /** Devuelve el status y el cuerpo sin lanzar por HTTP 500: WSAA informa sus errores así. */
    private static RespuestaHttp enviarSoap(String urlStr, String soapXml) throws AfipAuthException {
        try {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", "\"\"");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapXml.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) {
                return new RespuestaHttp(status, "");
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return new RespuestaHttp(status, sb.toString());
            }
        } catch (IOException e) {
            throw new AfipAuthException("red", "No se pudo comunicar con WSAA (" + urlStr + "): " + e.getMessage(), e);
        }
    }
}
