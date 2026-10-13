package com.prdv.rdv.iam.adapter.out.security;

import com.prdv.rdv.iam.application.port.output.BiometricVerifierPort;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Verification cryptographique des assertions biometriques (empreinte digitale,
 * Face ID, Passkey WebAuthn / FIDO2).
 *
 * <p>Supporte :
 * <ul>
 *   <li>Signature HMAC-SHA256 (Base64 URL-safe, Base64 standard ou Hex) calculee
 *       sur {@code challengeToken + ":" + credentialId} avec {@code publicKey},</li>
 *   <li>Empreinte SHA-256 deterministe {@code sha256(publicKey + ":" + challengeToken + ":" + credentialId)}
 *       pour les clients mobiles / tests d'integration.</li>
 * </ul>
 */
@Component
public class BiometricSignatureVerifierAdapter implements BiometricVerifierPort {

    @Override
    public boolean verifyAssertion(String publicKey,
                                   String challengeToken,
                                   String credentialId,
                                   String signature) {
        if (publicKey == null || publicKey.isBlank()
                || challengeToken == null || challengeToken.isBlank()
                || credentialId == null || credentialId.isBlank()
                || signature == null || signature.isBlank()) {
            return false;
        }
        String trimmedSig = signature.trim();
        String signedPayload = challengeToken.trim() + ":" + credentialId.trim();
        try {
            byte[] hmacBytes = hmacSha256(publicKey.trim(), signedPayload);
            String expectedHex = HexFormat.of().formatHex(hmacBytes);
            String expectedB64 = Base64.getEncoder().encodeToString(hmacBytes);
            String expectedB64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
            if (constantTimeEquals(trimmedSig, expectedHex)
                    || constantTimeEquals(trimmedSig, expectedB64)
                    || constantTimeEquals(trimmedSig, expectedB64Url)) {
                return true;
            }

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest((publicKey.trim() + ":" + signedPayload).getBytes(StandardCharsets.UTF_8));
            String shaHex = HexFormat.of().formatHex(digest);
            String shaB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return constantTimeEquals(trimmedSig, shaHex)
                    || constantTimeEquals(trimmedSig, shaB64);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] hmacSha256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
