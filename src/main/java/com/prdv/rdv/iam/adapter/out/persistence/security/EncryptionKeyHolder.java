package com.prdv.rdv.iam.adapter.out.persistence.security;

/**
 * Porte-acces statique vers la cle AES utilisee par {@link EncryptedStringConverter}.
 * Les converters JPA ne sont pas instancies par Spring : la cle est initialisee
 * au demarrage par l'adapteur {@code AesGcmCipherAdapter}.
 */
public final class EncryptionKeyHolder {

    private static volatile byte[] key;

    private EncryptionKeyHolder() {
    }

    public static void initialize(byte[] aesKey) {
        key = aesKey.clone();
    }

    public static byte[] getKey() {
        if (key == null) {
            throw new IllegalStateException("Cle de chiffrement non initialisee");
        }
        return key.clone();
    }
}
