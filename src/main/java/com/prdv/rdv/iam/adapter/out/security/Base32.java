package com.prdv.rdv.iam.adapter.out.security;

/**
 * Codec Base32 selon la RFC 4648 (alphabet A-Z2-7), sans padding pour les
 * secrets TOTP. Utilise par {@link TotpAdapter} afin d'eviter une dependance
 * externe supplementaire (Apache Commons Codec n'est pas impose par ailleurs).
 */
final class Base32 {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] DECODE_TABLE = new int[128];

    static {
        for (int i = 0; i < DECODE_TABLE.length; i++) {
            DECODE_TABLE[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length; i++) {
            DECODE_TABLE[ALPHABET[i]] = i;
        }
    }

    private Base32() {
    }

    static String encode(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1f;
                bitsLeft -= 5;
                result.append(ALPHABET[index]);
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1f;
            result.append(ALPHABET[index]);
        }
        return result.toString();
    }

    static byte[] decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return new byte[0];
        }
        String value = encoded.trim().replace("=", "").toUpperCase();
        byte[] output = new byte[value.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ' ' || c == '-' || c == '\n' || c == '\r') {
                continue;
            }
            if (c >= DECODE_TABLE.length || DECODE_TABLE[c] < 0) {
                throw new IllegalArgumentException("Caractere Base32 illegal : " + c);
            }
            buffer = (buffer << 5) | DECODE_TABLE[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        if (index == output.length) {
            return output;
        }
        byte[] trimmed = new byte[index];
        System.arraycopy(output, 0, trimmed, 0, index);
        return trimmed;
    }
}
