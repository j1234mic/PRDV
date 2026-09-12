package com.prdv.rdv.iam.adapter.out.security;

import com.prdv.rdv.iam.config.IamProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AesGcmCipherAdapterTest {

    private AesGcmCipherAdapter adapter() {
        IamProperties properties = new IamProperties();
        properties.getSecurity().getCipher().setSecret("cle-de-test-un-peu-longue-pour-aes");
        return new AesGcmCipherAdapter(properties);
    }

    @Test
    void encrypt_then_decrypt_round_trip() {
        AesGcmCipherAdapter adapter = adapter();
        String secret = "JBSWY3DPEHPK3PXP-TOTP-SECRET";
        String encrypted = adapter.encrypt(secret);
        assertNotEquals(secret, encrypted);
        assertEquals(secret, adapter.decrypt(encrypted));
    }

    @Test
    void two_encryptions_differ_but_both_decrypt() {
        AesGcmCipherAdapter adapter = adapter();
        String a = adapter.encrypt("valeur");
        String b = adapter.encrypt("valeur");
        assertNotEquals(a, b); // IV aleatoire
        assertEquals("valeur", adapter.decrypt(a));
        assertEquals("valeur", adapter.decrypt(b));
    }

    @Test
    void null_passthrough() {
        AesGcmCipherAdapter adapter = adapter();
        assertNull(adapter.encrypt(null));
        assertNull(adapter.decrypt(null));
    }
}
