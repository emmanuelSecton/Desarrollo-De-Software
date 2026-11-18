import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class AfipWsfeClient {

    private static final String URL_WSFE_HOMO = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";

    // 1. Consultar el último comprobante autorizado (FECompUltimoAutorizado)
    public static int obtenerUltimoComprobante(String token, String sign, long cuit, int ptoVta, int cbteTipo) throws Exception {
        String soapXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<ar:FECompUltimoAutorizado>" +
                "<ar:Auth>" +
                "<ar:Token>" + token + "</ar:Token>" +
                "<ar:Sign>" + sign + "</ar:Sign>" +
                "<ar:Cuit>" + cuit + "</ar:Cuit>" +
                "</ar:Auth>" +
                "<ar:PtoVta>" + ptoVta + "</ar:PtoVta>" +
                "<ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo>" +
                "</ar:FECompUltimoAutorizado>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        String response = enviarSoap(
                URL_WSFE_HOMO,
                "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado",
                soapXml
        );

        if (response.contains("<Err>")) {
            throw new RuntimeException("Error de ARCA al consultar último comprobante: " + response);
        }

        String nroStr = extraerValorTag(response, "CbteNro");
        return Integer.parseInt(nroStr);
    }

    // 2. Solicitar el CAE para la nueva factura (FECAESolicitar)
    public static String solicitarCae(String token, String sign, long cuit, int ptoVta, int cbteTipo, long nuevoNro, double importeTotal) throws Exception {
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String soapXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<ar:FECAESolicitar>" +
                "<ar:Auth>" +
                "<ar:Token>" + token + "</ar:Token>" +
                "<ar:Sign>" + sign + "</ar:Sign>" +
                "<ar:Cuit>" + cuit + "</ar:Cuit>" +
                "</ar:Auth>" +
                "<ar:FeCAEReq>" +
                "<ar:FeCabReq>" +
                "<ar:CantReg>1</ar:CantReg>" +
                "<ar:PtoVta>" + ptoVta + "</ar:PtoVta>" +
                "<ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo>" +
                "</ar:FeCabReq>" +
                "<ar:FeDetReq>" +
                "<ar:FECAEDetRequest>" +
                "<ar:Concepto>1</ar:Concepto>" +
                "<ar:DocTipo>99</ar:DocTipo>" +
                "<ar:DocNro>0</ar:DocNro>" +
                "<ar:CbteDesde>" + nuevoNro + "</ar:CbteDesde>" +
                "<ar:CbteHasta>" + nuevoNro + "</ar:CbteHasta>" +
                "<ar:CbteFch>" + fechaHoy + "</ar:CbteFch>" +
                "<ar:ImpTotal>" + importeTotal + "</ar:ImpTotal>" +
                "<ar:ImpTotConc>0.00</ar:ImpTotConc>" +
                "<ar:ImpNeto>" + importeTotal + "</ar:ImpNeto>" +
                "<ar:ImpOpEx>0.00</ar:ImpOpEx>" +
                "<ar:ImpTrib>0.00</ar:ImpTrib>" +
                "<ar:ImpIVA>0.00</ar:ImpIVA>" +
                "<ar:MonId>PES</ar:MonId>" +
                "<ar:MonCotiz>1</ar:MonCotiz>" +
                "<ar:CondicionIVAReceptorId>5</ar:CondicionIVAReceptorId>" +
                "</ar:FECAEDetRequest>" +
                "</ar:FeDetReq>" +
                "</ar:FeCAEReq>" +
                "</ar:FECAESolicitar>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        String response = enviarSoap(
                URL_WSFE_HOMO,
                "http://ar.gov.afip.dif.FEV1/FECAESolicitar",
                soapXml
        );

        String resultado = extraerValorTag(response, "Resultado");
        if ("A".equals(resultado)) {
            String cae = extraerValorTag(response, "CAE");
            String vtoCae = extraerValorTag(response, "CAEFchVto");
            return "¡Factura autorizada con éxito!\n" +
                    "Resultado: A (Aprobado)\n" +
                    "Comprobante Nro: " + nuevoNro + "\n" +
                    "CAE: " + cae + "\n" +
                    "Vencimiento CAE: " + vtoCae;
        } else {
            return "Rechazado o con observaciones de ARCA.\nXML Completo: " + response;
        }
    }

    private static String extraerValorTag(String xml, String tag) {
        String openTag = "<" + tag + ">";
        String closeTag = "</" + tag + ">";
        int start = xml.indexOf(openTag);
        int end = xml.indexOf(closeTag);
        if (start == -1 || end == -1) {
            return "";
        }
        return xml.substring(start + openTag.length(), end);
    }

    private static String enviarSoap(String urlStr, String soapAction, String soapXml) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "\"" + soapAction + "\"");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapXml.getBytes(StandardCharsets.UTF_8));
        }

        InputStream is = (conn.getResponseCode() >= 400) ? conn.getErrorStream() : conn.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            if (conn.getResponseCode() >= 400) {
                throw new RuntimeException("Error HTTP " + conn.getResponseCode() + " en WSFEv1: " + sb);
            }
            return sb.toString();
        }
    }
}
