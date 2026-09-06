package com.prdv.identity.application.service;

import com.prdv.identity.application.model.UserInfo;
import com.prdv.identity.application.port.in.VerifyEmailUseCase;
import com.prdv.identity.application.port.out.UserRepository;
import com.prdv.identity.domain.model.User;
import com.prdv.shared.event.DomainEventPublisher;
import com.prdv.shared.exception.NotFoundException;
import com.prdv.shared.exception.ValidationException;
import com.prdv.identity.domain.event.UserVerifiedEvent;

/** Verification du code OTP recu par email -> active le compte. */
public final class VerifyEmailHandler implements VerifyEmailUseCase {

    private final UserRepository users;
    private final OtpIssuer otpIssuer;
    private final DomainEventPublisher events;

    public VerifyEmailHandler(UserRepository users, OtpIssuer otpIssuer, DomainEventPublisher events) {
        this.users = users;
        this.otpIssuer = otpIssuer;
        this.events = events;
    }

    @Override
    public UserInfo verify(String email, String otpCode) {
        String normalized = email.toLowerCase().trim();
        User user = users.findByEmail(normalized)
                .orElseThrow(() -> new NotFoundException("Compte introuvable"));
        if (!otpIssuer.consume(normalized, otpCode)) {
            throw new ValidationException("Code OTP invalide ou expire");
        }
        user.verifyEmail();
        user = users.save(user);
        events.publish(new UserVerifiedEvent(user.id(), user.role()));
        return UserInfo.of(user);
    }
}
