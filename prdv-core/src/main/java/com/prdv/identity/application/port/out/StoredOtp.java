package com.prdv.identity.application.port.out;

import java.time.LocalDateTime;

/** OTP stocke (code hashe + expiration). */
public record StoredOtp(String codeHash, LocalDateTime expiresAt) {
}
