package com.prdv.identity.domain.event;

import com.prdv.identity.domain.model.Role;
import com.prdv.shared.event.DomainEvent;

/** Evenement emis apres verification email d'un compte (audit, onboarding...). */
public record UserVerifiedEvent(Long userId, Role role) implements DomainEvent {
}
