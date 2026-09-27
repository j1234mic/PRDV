package com.prdv.rdv.profile.adapter.out.iam;

import com.prdv.rdv.iam.application.port.output.CipherPort;
import com.prdv.rdv.iam.application.port.output.TokenizationPort;
import com.prdv.rdv.profile.application.port.output.SensitiveDataProtector;
import org.springframework.stereotype.Component;

/**
 * Adapteur : protection des donnees sensibles du contexte profils en
 * s'appuyant sur les primitives cryptographiques du contexte IAM
 * (tokenisation HMAC-SHA-256 deterministe, chiffrement AES-256-GCM).
 *
 * <p>Une seule cle, un seul algorithme pour toute la plateforme, sans que le
 * contexte profils n'en connaisse l'implementation.
 */
@Component
public class IamSensitiveDataProtectorAdapter implements SensitiveDataProtector {

    private final TokenizationPort tokenization;
    private final CipherPort cipher;

    public IamSensitiveDataProtectorAdapter(TokenizationPort tokenization, CipherPort cipher) {
        this.tokenization = tokenization;
        this.cipher = cipher;
    }

    @Override
    public String tokenize(String sensitiveValue, String namespace) {
        return tokenization.tokenize(sensitiveValue, namespace);
    }

    @Override
    public String mask(String sensitiveValue) {
        return tokenization.mask(sensitiveValue);
    }

    @Override
    public String encrypt(String plainText) {
        return cipher.encrypt(plainText);
    }

    @Override
    public String decrypt(String cipherText) {
        return cipher.decrypt(cipherText);
    }
}
