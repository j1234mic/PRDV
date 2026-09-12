package com.prdv.rdv.iam.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Comparaison de chaines en temps constant (anti-timing). */
final class MessageDigestUtil {

    private MessageDigestUtil() {
    }

    static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
