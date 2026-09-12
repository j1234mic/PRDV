package com.prdv.rdv.iam.application.port.output;

/**
 * Port de hachage des secrets :
 * <ul>
 *     <li>{@link #hash} / {@link #matches} : mots de passe (adaptateur BCrypt)</li>
 *     <li>{@link #digest} : empreinte deterministe de jetons aleatoires
 *         (OTP, refresh tokens) stockes en base - comparaison constante</li>
 * </ul>
 */
public interface PasswordHasher {

    String hash(String raw);

    boolean matches(String raw, String hashed);

    /** Empreinte SHA-256 hexadecimale (pour jetons aleatoires haute entropie). */
    String digest(String value);
}
