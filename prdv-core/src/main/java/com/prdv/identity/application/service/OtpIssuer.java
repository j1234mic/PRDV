package com.prdv.identity.application.service;

import com.prdv.identity.application.port.out.OtpSender;
import com.prdv.identity.application.port.out.OtpStore;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Service de domaine leger : emission/verification de codes OTP a usage commun
 * (inscription, changement d'email...). La generation du code est une regle metier,
 * l'envoi et le stockage sont des ports -> testable sans Spring (injection par constructeur).
 */
public final class OtpIssuer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpStore otpStore;
    private final OtpSender otpSender;
    private final Duration validity;
    private final Clock clock;

    public OtpIssuer(OtpStore otpStore, OtpSender otpSender, Duration validity, Clock clock) {
        this.otpStore = otpStore;
        this.otpSender = otpSender;
        this.validity = validity;
        this.clock = clock;
    }

    public String issue(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now(clock).plus(validity);
        otpStore.save(email, hash(code), expiresAt);
        otpSender.sendOtp(email, code);
        return code;
    }

    public boolean consume(String email, String code) {
        var stored = otpStore.find(email);
        if (stored.isEmpty()) {
            return false;
        }
        otpStore.delete(email);
        boolean valid = stored.get().expiresAt().isAfter(LocalDateTime.now(clock))
                && stored.get().codeHash().equals(hash(code));
        return valid;
    }

    /** Hash "maison" suffisant pour un OTP courte duree ; a durcir (bcrypt) si besoin. */
    private static String hash(String code) {
        return Integer.toHexString(code.hashCode()) + ":" + code.length();
    }
}
