package com.prdv.rdv.iam.adapter.out.persistence.security;

import com.prdv.rdv.iam.adapter.out.security.AesGcmCipherAdapter;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter JPA : chiffre / dechiffre automatiquement les colonnes sensibles
 * (ex : secret TOTP) en AES-256-GCM. Le prefixe {@value #PREFIX} distingue les
 * valeurs chiffrees et permet de rester tolerant avec d'anciennes valeurs.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    static final String PREFIX = "enc:v1:";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return PREFIX + AesGcmCipherAdapter.encryptStatic(attribute, EncryptionKeyHolder.getKey());
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (!dbData.startsWith(PREFIX)) {
            return dbData;
        }
        return AesGcmCipherAdapter.decryptStatic(dbData.substring(PREFIX.length()), EncryptionKeyHolder.getKey());
    }
}
