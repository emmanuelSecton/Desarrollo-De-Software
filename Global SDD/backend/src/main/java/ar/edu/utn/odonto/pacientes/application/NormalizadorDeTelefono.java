package ar.edu.utn.odonto.pacientes.application;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.util.Optional;

/**
 * Normaliza celulares a E.164 (D3). Sin prefijo internacional se asume Argentina, y un numero argentino
 * sin la marca de celular ({@code 9} o {@code 15}) se interpreta como celular: es el formato que exige WhatsApp.
 */
public final class NormalizadorDeTelefono {

    static final String CAMPO = "telefono";
    static final String MENSAJE = "no es un número de celular válido";
    private static final String REGION = "AR";
    private static final int CODIGO_ARGENTINA = 54;
    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private NormalizadorDeTelefono() {}

    /** @throws DatoDePacienteInvalidoException si no es un celular valido */
    public static String normalizar(String texto) {
        return intentar(texto).orElseThrow(() -> new DatoDePacienteInvalidoException(CAMPO, MENSAJE));
    }

    /** Version sin excepcion, usada por la busqueda para decidir si el texto es un telefono. */
    public static Optional<String> intentar(String texto) {
        if (texto == null || texto.isBlank() || texto.chars().filter(Character::isDigit).count() < 8) {
            return Optional.empty();
        }
        try {
            PhoneNumber numero = UTIL.parse(texto, REGION);
            if (numero.getCountryCode() == CODIGO_ARGENTINA) {
                String nacional = Long.toString(numero.getNationalNumber());
                if (!nacional.startsWith("9")) {
                    numero = UTIL.parse("+549" + nacional, REGION);
                }
            }
            if (!UTIL.isValidNumber(numero) || !esCelular(UTIL.getNumberType(numero))) {
                return Optional.empty();
            }
            return Optional.of(UTIL.format(numero, PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }

    private static boolean esCelular(PhoneNumberType tipo) {
        return tipo == PhoneNumberType.MOBILE || tipo == PhoneNumberType.FIXED_LINE_OR_MOBILE;
    }
}
