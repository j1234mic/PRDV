package com.prdv.profile.domain.event;

import com.prdv.shared.event.DomainEvent;

/** Decision d'un moderateur sur un profil praticien -> notifie le interesse (Observer). */
public record DoctorProfileValidatedEvent(Long doctorUserId, boolean approved, String reason) implements DomainEvent {
}
