public class Main {
    public static void main(String[] args) {
        try {
            String keyPath = "privada_test.key";
            String certPath = "certificado_test.crt";
            long cuit = 20449853564L; // Tu CUIT sin guiones
            int puntoDeVenta = 1;     // Punto de venta de prueba
            int tipoComprobante = 11; // 11 = Factura C (como en la pág. 8 de la guía)

            System.out.println("1. Solicitando Token y Sign a WSAA...");
            String[] auth = AfipWsaaClient.obtenerTokenYSign(keyPath, certPath);
            String token = auth[0];
            String sign = auth[1];
            System.out.println("Autenticado OK. Token activo.");

            System.out.println("\n2. Consultando último comprobante autorizado en WSFEv1...");
            int ultimoNro = AfipWsfeClient.obtenerUltimoComprobante(token, sign, cuit, puntoDeVenta, tipoComprobante);
            int nuevoNro = ultimoNro + 1;
            System.out.println("Último número registrado: " + ultimoNro + ". Siguiente a emitir: " + nuevoNro);

            System.out.println("\n3. Enviando factura para obtener CAE...");
            String respuestaCae = AfipWsfeClient.solicitarCae(token, sign, cuit, puntoDeVenta, tipoComprobante, nuevoNro, 12100.00);

            System.out.println("\n--- Respuesta WSFEv1 ---");
            System.out.println(respuestaCae);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
