package com.prdv.adapters.out.security;

import com.prdv.identity.application.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** ADAPTATEUR de PasswordHasher. Coût 10 : compromis securite/UX recommande pour un SaaS sante (a auditer). */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null) {
            return false;
        }
        return encoder.matches(rawPassword, hash);
    }
}
