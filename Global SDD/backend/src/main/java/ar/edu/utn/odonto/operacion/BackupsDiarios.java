package ar.edu.utn.odonto.operacion;

public final class BackupsDiarios {

    private BackupsDiarios() {}

    public static void main(String[] args) {
        System.exit(ejecutar(args));
    }

    public static int ejecutar(String[] args) {
        if (args.length != 1) {
            System.err.println("uso: BackupsDiarios <base>");
            return 1;
        }
        PoliticaBackups.Resultado resultado = PoliticaBackups.verificar(1);
        System.err.println("ALERTA: " + resultado.mensaje() + " en " + args[0]);
        return 1;
    }
}
