package com.prdv.rdv.iam.application.port.output;

/**
 * Tokenisation de donnees tres sensibles (RIB/IBAN, numero de piece d'identite).
 *
 * <p>La valeur brute ne quitte jamais la couche de securite : un jeton deterministe
 * et non reversible ({@code tok_xxxx}) est stocke en base. Les donnees reelles
 * peuvent ainsi etre conservees dans un coffre-fort externe (HSM / vault) sans
 * changer le domaine (principe Open/Closed).
 */
public interface TokenizationPort {

    /** Retourne un jeton stable pour une valeur et un namespace (ex : « IBAN », « IDENTITY_DOC »). */
    String tokenize(String sensitiveValue, String namespace);

    /** Valeur masquee pour affichage (ex : FR76 **** **** **** 123). */
    String mask(String sensitiveValue);
}
