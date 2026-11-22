package ar.edu.utn.tp2.arca;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;

@Component
public class AfipWsaaClient {

    @Value("${arca.key-path}")
    private String keyPath;

    @Value("${arca.cert-path}")
    private String certPath;

    @Value("${arca.wsaa-url}")
    private String wsaaUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Caché simple para reutilizar el Ticket de Acceso mientras esté vigente
    private String cachedToken;
    private String cachedSign;
    private ZonedDateTime cachedExpiration;

    public synchronized String[] obtenerTokenYSign() throws Exception {
        if (cachedToken != null && cachedExpiration != null && ZonedDateTime.now().isBefore(cachedExpiration.minusMinutes(2))) {
            return new String[]{cachedToken, cachedSign};
        }

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        PrivateKey privateKey = cargarClavePrivada(keyPath);
        X509Certificate cert = cargarCertificado(certPath);

        ZonedDateTime now = ZonedDateTime.now();
        String genTime = now.minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime exp = now.plusMinutes(10);
        String expTime = exp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String traXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<loginTicketRequest version=\"1.0\">" +
                "<header>" +
                "<uniqueId>" + (System.currentTimeMillis() / 1000) + "</uniqueId>" +
                "<generationTime>" + genTime + "</generationTime>" +
                "<expirationTime>" + expTime + "</expirationTime>" +
                "</header>" +
                "<service>wsfe</service>" +
                "</loginTicketRequest>";

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC").build(privateKey);

        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                .build(sha256Signer, cert));
        gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));

        CMSSignedData signedData = gen.generate(
                new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8)), true);
        String cmsBase64 = Base64.getEncoder().encodeToString(signedData.getEncoded());

        String soapEnvelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:wsaa=\"http://wsaa.view.sua.dvadac.desein.afip.gov\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body><wsaa:loginCms><wsaa:in0>" + cmsBase64 + "</wsaa:in0></wsaa:loginCms></soapenv:Body>" +
                "</soapenv:Envelope>";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wsaaUrl))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String xmlLimpio = response.body().replace("&lt;", "<").replace("&gt;", ">");

        if (response.statusCode() != 200 || !xmlLimpio.contains("<token>")) {
            throw new RuntimeException("Error en WSAA (HTTP " + response.statusCode() + "): " + xmlLimpio);
        }

        cachedToken = extraerTag(xmlLimpio, "token");
        cachedSign = extraerTag(xmlLimpio, "sign");
        cachedExpiration = exp;

        return new String[]{cachedToken, cachedSign};
    }

    private PrivateKey cargarClavePrivada(String path) throws Exception {
        try (PEMParser pemParser = new PEMParser(new FileReader(path))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (object instanceof PEMKeyPair keyPair) {
                return converter.getKeyPair(keyPair).getPrivate();
            } else if (object instanceof PrivateKeyInfo keyInfo) {
                return converter.getPrivateKey(keyInfo);
            }
            throw new IllegalArgumentException("Formato de clave privada no soportado: " + object);
        }
    }

    private X509Certificate cargarCertificado(String path) throws Exception {
        try (FileInputStream fis = new FileInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fis);
        }
    }

    private String extraerTag(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = xml.indexOf(open);
        int end = xml.indexOf(close);
        if (start == -1 || end == -1) return null;
        return xml.substring(start + open.length(), end).trim();
    }
}