package com.prdv.rdv.iam.adapter.out.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vecteurs de test RFC 6238 (secret de reference "12345678901234567890",
 * code attendu a t=59s : 287082).
 */
class TotpAdapterTest {

    private static final String RFC_SECRET_B32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private final TotpAdapter totp = new TotpAdapter();

    @Test
    void rfc6238_test_vector_at_counter_1() {
        assertEquals("287082", totp.generate(RFC_SECRET_B32, 1L));
    }

    @Test
    void generated_secret_is_verified() {
        String secret = totp.generateSecret();
        assertNotNull(secret);
        assertTrue(secret.matches("[A-Z2-7]{28,32}"));
        long currentCounter = Instant.now().getEpochSecond() / 30;
        assertTrue(totp.verifyCode(secret, totp.generate(secret, currentCounter)));
    }

    @Test
    void provisioning_uri_contains_issuer_and_secret() {
        String uri = totp.provisioningUri("TESTSECRET", "dr.dupont@prdv.app");
        assertTrue(uri.startsWith("otpauth://totp/PRDV:dr.dupont%40prdv.app"));
        assertTrue(uri.contains("issuer=PRDV"));
        assertTrue(uri.contains("secret=TESTSECRET"));
    }

    @Test
    void invalid_code_is_rejected() {
        assertFalse(totp.verifyCode(totp.generateSecret(), "abc"));
        assertFalse(totp.verifyCode(totp.generateSecret(), "000000"));
    }
}
