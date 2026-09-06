package com.prdv.identity.application.service;

import com.prdv.identity.application.model.UserInfo;
import com.prdv.identity.application.port.in.RegisterUserUseCase;
import com.prdv.identity.application.port.out.PasswordHasher;
import com.prdv.identity.application.port.out.UserRepository;
import com.prdv.identity.domain.model.PasswordHash;
import com.prdv.identity.domain.model.Role;
import com.prdv.identity.domain.model.User;
import com.prdv.shared.exception.ConflictException;
import com.prdv.shared.exception.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Implementation du cas d'usage "Inscription" (module 1.1).
 * SRP : cette classe NE FAIT QU'orchestrer ; les regles sont dans User, l'envoi OTP dans OtpIssuer.
 * Le handler est une FACADE applicative, cadre-independent (pas d'annotation Spring ici).
 */
public final class RegisterUserHandler implements RegisterUserUseCase {

    /** Roles auto-inscriptibles : SECRETARY/ADMIN passent par l'onboarding interne (roadmap admin). */
    private static final Set<Role> SELF_SERVICE_ROLES = Set.of(Role.PATIENT, Role.DOCTOR);

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final OtpIssuer otpIssuer;
    private final Clock clock;

    public RegisterUserHandler(UserRepository users, PasswordHasher passwordHasher,
                               OtpIssuer otpIssuer, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.otpIssuer = otpIssuer;
        this.clock = clock;
    }

    @Override
    public UserInfo register(Command command) {
        Role role = parseRole(command.role());
        if (!SELF_SERVICE_ROLES.contains(role)) {
            throw new ValidationException("Role non inscriptible en libre-service : " + command.role());
        }
        String email = normalize(command.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Un compte existe deja avec cet email");
        }
        validatePassword(command.password());

        User user = User.register(email, command.phone(),
                new PasswordHash(passwordHasher.hash(command.password())), role,
                LocalDateTime.now(clock));
        user = users.save(user);
        otpIssuer.issue(user.email());
        return UserInfo.of(user);
    }

    private static Role parseRole(String raw) {
        try {
            return Role.from(raw);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Role inconnu : " + raw);
        }
    }

    private static String normalize(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ValidationException("Email invalide");
        }
        return email.toLowerCase().trim();
    }

    private static void validatePassword(String raw) {
        if (raw == null || raw.length() < 10) {
            throw new ValidationException("Mot de passe trop court (10 caracteres min, recommandation ANSSI)");
        }
    }
}
