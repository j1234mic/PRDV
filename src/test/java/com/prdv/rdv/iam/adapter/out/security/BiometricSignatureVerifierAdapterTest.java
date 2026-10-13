package com.prdv.rdv.iam.adapter.out.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiometricSignatureVerifierAdapterTest {

    private final BiometricSignatureVerifierAdapter verifier = new BiometricSignatureVerifierAdapter();

    @Test
    void verifies_deterministic_sha256_biometric_assertion() throws Exception {
        String publicKey = "pub-key-faceid-iphone16";
        String challengeToken = "jwt-challenge-123";
        String credentialId = "cred-faceid-001";

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest(
                (publicKey + ":" + challengeToken + ":" + credentialId).getBytes(StandardCharsets.UTF_8));
        String signatureHex = HexFormat.of().formatHex(digest);

        assertTrue(verifier.verifyAssertion(publicKey, challengeToken, credentialId, signatureHex));
        assertFalse(verifier.verifyAssertion(publicKey, challengeToken, credentialId, "invalid-signature"));
    }
}
