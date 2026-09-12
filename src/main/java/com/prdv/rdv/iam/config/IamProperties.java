package com.prdv.rdv.iam.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du module IAM (prefixe {@code prdv}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "prdv")
public class IamProperties {

    private Security security = new Security();
    private Storage storage = new Storage();
    private Admin admin = new Admin();
    private Frontend frontend = new Frontend();

    @Getter
    @Setter
    public static class Security {
        private Jwt jwt = new Jwt();
        private Refresh refresh = new Refresh();
        private Login login = new Login();
        private Otp otp = new Otp();
        private Cipher cipher = new Cipher();
        private RateLimit rateLimit = new RateLimit();

        @Getter
        @Setter
        public static class Jwt {
            private String secret = "dev-only-secret";
            private long accessTokenTtlSeconds = 900;
            private long challengeTokenTtlSeconds = 300;
        }

        @Getter
        @Setter
        public static class Refresh {
            private long tokenTtlDays = 14;
        }

        @Getter
        @Setter
        public static class Login {
            private int maxFailedAttempts = 5;
            private int lockDurationMinutes = 15;
        }

        @Getter
        @Setter
        public static class Otp {
            private long ttlSeconds = 300;
            private long resendCooldownSeconds = 60;
        }

        @Getter
        @Setter
        public static class Cipher {
            private String secret = "dev-cipher-secret";
        }

        @Getter
        @Setter
        public static class RateLimit {
            private int loginAttemptsPerMinute = 10;
            private int otpPerMinute = 5;
        }
    }

    @Getter
    @Setter
    public static class Storage {
        private String localPath = "./storage/kyc";
    }

    @Getter
    @Setter
    public static class Admin {
        private String email = "superadmin@prdv.app";
        private String password = "Admin#2026!";
        private String firstName = "Super";
        private String lastName = "Administrator";
    }

    @Getter
    @Setter
    public static class Frontend {
        private String oauthRedirectUrl = "http://localhost:3000/auth/callback";
    }
}
