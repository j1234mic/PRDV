package com.prdv.rdv.iam.domain.model.auth;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Challenge OTP : code a usage unique (a expiration), le code brut n'est jamais
 * conserve : seul son hash est stocke. Sert a l'inscription, la double
 * authentification a la connexion et la verification SMS.
 */
@Getter
@Setter
public class OtpChallenge {

    public enum Channel { EMAIL, SMS }

    public enum Purpose {
        /** Confirmation du email / telephone lors de l'inscription. */
        REGISTRATION,
        /** Deuxieme facteur declenche a la connexion (MFA OTP, fraude detectee). */
        LOGIN,
        STEP_UP,
        PASSWORD_RESET
    }

    private static final int MAX_ATTEMPTS = 3;

    private Long id;
    /** Email ou numero de telephone destinataire. */
    private String target;
    private Channel channel;
    private Purpose purpose;
    private String codeHash;
    private Instant expiresAt;
    private Instant createdAt;
    private int attempts;
    private boolean consumed;

    public static OtpChallenge issue(String target, Channel channel, Purpose purpose,
                                     String codeHash, Duration ttl, Clock clock) {
        OtpChallenge c = new OtpChallenge();
        c.target = target;
        c.channel = channel;
        c.purpose = purpose;
        c.codeHash = codeHash;
        c.createdAt = clock.instant();
        c.expiresAt = c.createdAt.plus(ttl);
        return c;
    }

    /**
     * Verifie le code : augmente le nombre de tentatives, marque la consommation.
     * La comparaison cryptographique du code est faite par l'appelant.
     */
    public void verify(boolean codeMatches, Clock clock) {
        if (consumed) {
            throw IamException.of(IamErrorCode.OTP_INVALID, "Code deja utilise");
        }
        if (clock.instant().isAfter(expiresAt)) {
            throw IamException.of(IamErrorCode.OTP_EXPIRED, "Code expire, redemandez-en un nouveau");
        }
        if (!codeMatches) {
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                consumed = true;
            }
            throw IamException.of(IamErrorCode.OTP_INVALID,
                    "Code incorrect (" + (MAX_ATTEMPTS - attempts) + " essai(s) restant(s))");
        }
        this.consumed = true;
    }
}
