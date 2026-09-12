package com.prdv.rdv.iam.adapter.out.security;

import com.prdv.rdv.iam.application.port.output.RandomCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generateur cryptographiquement sur : OTP numeriques et jetons opaques.
 */
@Component
public class SecureRandomCodeGenerator implements RandomCodeGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String numericCode(int digits) {
        StringBuilder sb = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    @Override
    public String opaqueToken() {
        byte[] bytes = new byte[48]; // 384 bits d'entropie, URL-safe
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
