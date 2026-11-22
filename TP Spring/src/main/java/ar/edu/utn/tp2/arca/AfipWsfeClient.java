package ar.edu.utn.tp2.arca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Component
public class AfipWsfeClient {

    @Value("${arca.cuit}")
    private long cuit;

    @Value("${arca.wsfe-url}")
    private String wsfeUrl;

    @Value("${arca.cbte-tipo:11}")
    private int cbteTipo;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public long obtenerUltimoComprobante(String token, String sign, int ptoVta) throws Exception {
        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<ar:FECompUltimoAutorizado>" +
                "<ar:Auth><ar:Token>" + token + "</ar:Token><ar:Sign>" + sign + "</ar:Sign><ar:Cuit>" + cuit + "</ar:Cuit></ar:Auth>" +
                "<ar:PtoVta>" + ptoVta + "</ar:PtoVta>" +
                "<ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo>" +
                "</ar:FECompUltimoAutorizado>" +
                "</soapenv:Body></soapenv:Envelope>";

        String resp = enviarSoap(soap, "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado");
        String cbteNro = extraerTag(resp, "CbteNro");
        if (cbteNro == null) {
            throw new RuntimeException("No se pudo obtener el último comprobante de ARCA: " + resp);
        }
        return Long.parseLong(cbteNro);
    }

    public AfipCaeResponse solicitarCae(String token, String sign, int ptoVta, Date fechaEmision,
                                        double importeTotal, String cuitCliente) throws Exception {
        long ultimoNro = obtenerUltimoComprobante(token, sign, ptoVta);
        long nuevoNro = ultimoNro + 1;

        String fechaStr = new SimpleDateFormat("yyyyMMdd").format(fechaEmision != null ? fechaEmision : new Date());
        String totalStr = String.format(Locale.US, "%.2f", importeTotal);

        int docTipo = 99; // Consumidor Final
        long docNro = 0L;
        int condIvaReceptor = 5; // 5 = Consumidor Final

        if (cuitCliente != null && !cuitCliente.isBlank()) {
            String soloDigitos = cuitCliente.replaceAll("\\D", "");
            if (soloDigitos.length() == 11) {
                docTipo = 80; // CUIT
                docNro = Long.parseLong(soloDigitos);
                condIvaReceptor = 1; // 1 = IVA Responsable Inscripto
            }
        }

        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<ar:FECAESolicitar>" +
                "<ar:Auth><ar:Token>" + token + "</ar:Token><ar:Sign>" + sign + "</ar:Sign><ar:Cuit>" + cuit + "</ar:Cuit></ar:Auth>" +
                "<ar:FeCAEReq>" +
                "<ar:FeCabReq><ar:CantReg>1</ar:CantReg><ar:PtoVta>" + ptoVta + "</ar:PtoVta><ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo></ar:FeCabReq>" +
                "<ar:FeDetReq><ar:FECAEDetRequest>" +
                "<ar:Concepto>1</ar:Concepto>" +
                "<ar:DocTipo>" + docTipo + "</ar:DocTipo>" +
                "<ar:DocNro>" + docNro + "</ar:DocNro>" +
                "<ar:CbteDesde>" + nuevoNro + "</ar:CbteDesde>" +
                "<ar:CbteHasta>" + nuevoNro + "</ar:CbteHasta>" +
                "<ar:CbteFch>" + fechaStr + "</ar:CbteFch>" +
                "<ar:ImpTotal>" + totalStr + "</ar:ImpTotal>" +
                "<ar:ImpTotConc>0.00</ar:ImpTotConc>" +
                "<ar:ImpNeto>" + totalStr + "</ar:ImpNeto>" +
                "<ar:ImpOpEx>0.00</ar:ImpOpEx>" +
                "<ar:ImpTrib>0.00</ar:ImpTrib>" +
                "<ar:ImpIVA>0.00</ar:ImpIVA>" +
                "<ar:MonId>PES</ar:MonId>" +
                "<ar:MonCotiz>1</ar:MonCotiz>" +
                "<ar:CondicionIVAReceptorId>" + condIvaReceptor + "</ar:CondicionIVAReceptorId>" +
                "</ar:FECAEDetRequest></ar:FeDetReq>" +
                "</ar:FeCAEReq>" +
                "</ar:FECAESolicitar>" +
                "</soapenv:Body></soapenv:Envelope>";

        String resp = enviarSoap(soap, "http://ar.gov.afip.dif.FEV1/FECAESolicitar");

        String resultado = extraerTag(resp, "Resultado");
        String cae = extraerTag(resp, "CAE");
        String vto = extraerTag(resp, "CAEFchVto");
        String msgError = extraerTag(resp, "Msg");

        return new AfipCaeResponse(resultado, cae, vto, nuevoNro, msgError);
    }

    private String enviarSoap(String soapXml, String soapAction) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wsfeUrl))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", soapAction)
                .POST(HttpRequest.BodyPublishers.ofString(soapXml, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error en WSFEv1 (HTTP " + response.statusCode() + "): " + response.body());
        }
        return response.body();
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