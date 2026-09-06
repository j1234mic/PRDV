package com.prdv.identity;

import com.prdv.identity.domain.model.PasswordHash;
import com.prdv.identity.domain.model.Role;
import com.prdv.identity.domain.model.User;
import com.prdv.identity.domain.model.UserStatus;
import com.prdv.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Politique de blocage apres tentatives (module 1.2) : regle de domaine pure. */
class UserLockoutTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 9, 7, 8, 0);

    private User user() {
        User u = User.register("alice@example.org", "+33612345678", new PasswordHash("$2a$x"), Role.PATIENT, T0);
        u.verifyEmail();
        return u;
    }

    @Test
    void locks_after_third_failure_and_unlocks_after_cooldown() {
        User u = user();
        for (int i = 0; i < 3; i++) {
            u.registerFailedAttempt(T0.plusMinutes(i), 3, Duration.ofMinutes(15));
        }
        assertEquals(UserStatus.LOCKED, u.status());
        // bloque jusqu'a (T0+2min)+15min = T0+17min
        assertThrows(DomainException.class, () -> u.ensureCanAuthenticate(T0.plusMinutes(5)));
        assertThrows(DomainException.class, () -> u.ensureCanAuthenticate(T0.plusMinutes(16)));

        u.unlockIfExpired(T0.plusMinutes(18));
        assertEquals(UserStatus.ACTIVE, u.status());
        u.ensureCanAuthenticate(T0.plusMinutes(19)); // ne leve pas
    }

    @Test
    void successful_login_resets_counters() {
        User u = user();
        u.registerFailedAttempt(T0, 3, Duration.ofMinutes(15));
        u.resetFailedAttempts();
        assertEquals(0, u.failedAttempts());
        assertEquals(UserStatus.ACTIVE, u.status());
    }
}
