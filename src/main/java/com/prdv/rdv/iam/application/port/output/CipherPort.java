package com.prdv.rdv.iam.application.port.output;

/**
 * Chiffrement symetrique de donnees sensibles (secrets TOTP, etc.).
 * Implementation : AES-GCM (chiffrement authentifie).
 * C'est aussi la brique du chiffrement « at rest » via un converter JPA.
 */
public interface CipherPort {

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
