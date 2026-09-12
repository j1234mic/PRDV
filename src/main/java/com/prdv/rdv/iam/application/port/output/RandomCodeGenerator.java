package com.prdv.rdv.iam.application.port.output;

/**
 * Generation de secrets cryptographiques (OTP numeriques, jetons de refresh,
 * identifiants de famille de tokens). Isole {@link java.security.SecureRandom}
 * derriere un port pour la testabilite.
 */
public interface RandomCodeGenerator {

    /** Code OTP numerique de {@code digits} chiffres. */
    String numericCode(int digits);

    /** Jeton opaque URL-safe (256 bits d'entropie). */
    String opaqueToken();
}
