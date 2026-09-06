package com.prdv.identity.application.service;

import com.prdv.identity.application.model.AuthTokens;
import com.prdv.identity.application.port.in.LoginUseCase;
import com.prdv.identity.application.port.out.PasswordHasher;
import com.prdv.identity.application.port.out.TokenIssuer;
import com.prdv.identity.application.port.out.UserRepository;
import com.prdv.identity.domain.event.LoginFailedEvent;
import com.prdv.identity.domain.model.User;
import com.prdv.identity.domain.policy.AccountLockoutPolicy;
import com.prdv.shared.event.DomainEventPublisher;
import com.prdv.shared.exception.DomainException;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Authentification (module 1.2). Le blocage anti-force-brute est delegue a
 * AccountLockoutPolicy (STRATEGY) : remplacer la politique ne touche pas ce handler.
 * Secret : message d'erreur identique quel que soit le motif (pas d'enumeration de comptes).
 */
public final class LoginHandler implements LoginUseCase {

    private static final String GENERIC_FAILURE = "Email ou mot de passe incorrect";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final AccountLockoutPolicy lockoutPolicy;
    private final DomainEventPublisher events;
    private final Clock clock;

    public LoginHandler(UserRepository users, PasswordHasher passwordHasher, TokenIssuer tokenIssuer,
                        AccountLockoutPolicy lockoutPolicy, DomainEventPublisher events, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.lockoutPolicy = lockoutPolicy;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public AuthTokens login(Command command) {
        String email = command.email() == null ? "" : command.email().toLowerCase().trim();
        LocalDateTime now = LocalDateTime.now(clock);

        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            events.publish(new LoginFailedEvent(email, "unknown_email", now));
            throw new DomainException(GENERIC_FAILURE);
        }
        user.ensureCanAuthenticate(now);
        if (!passwordHasher.matches(command.password(), user.passwordHash().value())) {
            lockoutPolicy.onFailedAttempt(user, now);
            users.save(user);
            events.publish(new LoginFailedEvent(email, "bad_password", now));
            throw new DomainException(GENERIC_FAILURE);
        }
        lockoutPolicy.onSuccess(user);
        users.save(user);
        return tokenIssuer.issue(user);
    }
}
