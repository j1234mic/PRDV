package com.prdv.rdv.iam.application.result;

/**
 * Resultats d'authentification.
 * Une connexion peut aboutir directement (jetons) ou requerrir un deuxieme
 * facteur (TOTP ou OTP) : {@link AuthResult} porte les deux cas de figure.
 */
public final class AuthResults {

    private AuthResults() {
    }

    public record TokenSet(String accessToken,
                           String refreshToken,
                           String tokenType,
                           long expiresInSeconds) {
        public static TokenSet bearer(String accessToken, String refreshToken, long expiresInSeconds) {
            return new TokenSet(accessToken, refreshToken, "Bearer", expiresInSeconds);
        }
    }

    public record Challenge(String challengeToken, String requiredFactor, String target) {
    }

    public record AuthResult(TokenSet tokens, Challenge challenge) {

        public static AuthResult authenticated(TokenSet tokens) {
            return new AuthResult(tokens, null);
        }

        public static AuthResult challengeRequired(Challenge challenge) {
            return new AuthResult(null, challenge);
        }

        public boolean isAuthenticated() {
            return tokens != null;
        }
    }

    public record MfaSetup(String secret, String otpAuthUri) {
    }
}
