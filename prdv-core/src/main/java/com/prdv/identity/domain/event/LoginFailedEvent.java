package com.prdv.identity.domain.event;

import com.prdv.shared.event.DomainEvent;

import java.time.LocalDateTime;

/** Alimente la detection de connexions suspectes / historique des connexions (module 1.2). */
public record LoginFailedEvent(String email, String reason, LocalDateTime at) implements DomainEvent {
}
