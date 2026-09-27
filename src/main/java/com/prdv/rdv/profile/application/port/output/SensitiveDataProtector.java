package com.prdv.rdv.profile.application.port.output;

/**
 * Port de sortie : protection des donnees sensibles du module profils
 * (numero de securite sociale, IBAN professionnel).
 *
 * <p>Deux operations complementaires :
 * <ul>
 *     <li>{@link #tokenize} : jeton deterministe non reversible, utilisable
 *     comme cle de recherche sans exposer la valeur ;</li>
 *     <li>{@link #mask} : valeur affichable partiellement masquee.</li>
 * </ul>
 * L'adapteur fait le pont avec les primitives cryptographiques du module IAM
 * (HMAC-SHA-256), sans que le contexte profils n'en depende directement.
 */
public interface SensitiveDataProtector {

    String tokenize(String sensitiveValue, String namespace);

    String mask(String sensitiveValue);

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
