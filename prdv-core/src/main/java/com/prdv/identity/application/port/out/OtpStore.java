package com.prdv.identity.application.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

/** Port de sortie : stockage temporaire des codes OTP (adaptateur MySQL/TTL ici, Redis en prod). */
public interface OtpStore {
    void save(String email, String codeHash, LocalDateTime expiresAt);
    Optional<StoredOtp> find(String email);
    void delete(String email);
}
