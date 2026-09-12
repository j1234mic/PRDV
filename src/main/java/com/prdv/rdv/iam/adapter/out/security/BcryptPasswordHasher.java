package com.prdv.rdv.iam.adapter.out.security;

import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.service.support.OtpIssuer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapteur de hachage :
 * <ul>
 *   <li>BCrypt pour les mots de passe (sel adaptative, resistant hors-ligne)</li>
 *   <li>SHA-256 pour les jetons aleatoires haute entropie (OTP, refresh tokens)</li>
 * </ul>
 */
@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(11);

    @Override
    public String hash(String raw) {
        return passwordEncoder.encode(raw);
    }

    @Override
    public boolean matches(String raw, String hashed) {
        if (raw == null || hashed == null) {
            return false;
        }
        if (hashed.startsWith("$2")) {
            return passwordEncoder.matches(raw, hashed);
        }
        // Cas des empreintes SHA-256 (OTP, jetons)
        return MessageDigestUtil.constantTimeEquals(digest(raw), hashed);
    }

    @Override
    public String digest(String value) {
        return OtpIssuer.sha256Hex(value);
    }
}
