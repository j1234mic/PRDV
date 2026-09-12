package com.prdv.rdv.iam.adapter.out.security;

import com.prdv.rdv.iam.application.port.output.TotpPort;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Implementation TOTP RFC 6238 (compatible Google Authenticator, Authy...).
 * HMAC-SHA1, pas de 30 s, code a 6 chiffres, fenetre de tolerance +/- 1 pas.
 */
@Component
public class TotpAdapter implements TotpPort {

    private static final int SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final String ISSUER = "PRDV";

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    @Override
    public String provisioningUri(String secret, String account) {
        String label = ISSUER + ":" + URLEncoder.encode(account, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label + "?issuer=" + ISSUER + "&secret=" + secret;
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentCounter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (long window = -1; window <= 1; window++) {
            String candidate = generate(secret, currentCounter + window);
            if (constantTimeEquals(candidate, code)) {
                return true;
            }
        }
        return false;
    }

    String generate(String base32Secret, long counter) {
        byte[] key = Base32.decode(base32Secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xff);
            counter >>= 8;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Generation TOTP impossible", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    /** Codec Base32 (RFC 4648) sans dependance externe. */
    static final class Base32 {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        private static final int[] LOOKUP = new int[128];

        static {
            for (int i = 0; i < ALPHABET.length(); i++) {
                LOOKUP[ALPHABET.charAt(i)] = i;
            }
        }

        private Base32() {
        }

        static String encode(byte[] bytes) {
            StringBuilder result = new StringBuilder();
            int buffer = 0;
            int bitsLeft = 0;
            for (byte b : bytes) {
                buffer = (buffer << 8) | (b & 0xff);
                bitsLeft += 8;
                while (bitsLeft >= 5) {
                    int index = (buffer >> (bitsLeft - 5)) & 0x1f;
                    bitsLeft -= 5;
                    result.append(ALPHABET.charAt(index));
                }
            }
            if (bitsLeft > 0) {
                result.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1f));
            }
            return result.toString();
        }

        static byte[] decode(String encoded) {
            String clean = encoded.trim().replace("=", "").toUpperCase();
            int buffer = 0;
            int bitsLeft = 0;
            byte[] output = new byte[clean.length() * 5 / 8];
            int index = 0;
            for (char c : clean.toCharArray()) {
                if (c >= LOOKUP.length || LOOKUP[c] == 0 && c != 'A') {
                    throw new IllegalArgumentException("Caractere Base32 invalide : " + c);
                }
                buffer = (buffer << 5) | LOOKUP[c];
                bitsLeft += 5;
                if (bitsLeft >= 8) {
                    output[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                    bitsLeft -= 8;
                }
            }
            return output;
        }
    }
}
