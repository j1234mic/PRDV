package com.prdv.rdv.iam.domain;

import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.Email;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLockoutTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T10:00:00Z"), ZoneOffset.UTC);

    private User newUser() {
        return User.register(Email.of("patient@prdv.app"), null, "$2$hash",
                ProfileType.PATIENT, clock);
    }

    @Test
    void locks_account_after_five_failed_attempts() {
        User user = newUser();
        for (int i = 0; i < 4; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15), clock);
        }
        assertEquals(AccountStatus.PENDING_EMAIL_VERIFICATION, user.getStatus());
        assertFalse(user.isCurrentlyLocked(clock));

        user.recordFailedLogin(5, Duration.ofMinutes(15), clock);
        assertEquals(AccountStatus.LOCKED, user.getStatus());
        assertTrue(user.isCurrentlyLocked(clock));
    }

    @Test
    void successful_login_resets_failures_and_unlocks() {
        User user = newUser();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15), clock);
        }
        user.resetFailedLogins(clock);
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedAttempts());
        assertFalse(user.isCurrentlyLocked(clock));
    }

    @Test
    void lock_expires_after_duration() {
        User user = newUser();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15), clock);
        }
        Clock later = Clock.fixed(Instant.parse("2026-09-12T10:20:00Z"), ZoneOffset.UTC);
        assertTrue(user.isCurrentlyLocked(clock));
        assertFalse(user.isCurrentlyLocked(later));
    }

    @Test
    void revoking_tokens_increments_version() {
        User user = newUser();
        long before = user.getTokenVersion();
        user.revokeAllTokens(clock);
        assertEquals(before + 1, user.getTokenVersion());
    }
}
