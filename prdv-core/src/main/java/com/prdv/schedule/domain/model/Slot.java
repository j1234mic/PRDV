package com.prdv.schedule.domain.model;

import java.time.LocalDateTime;

/**
 * Value Object : un creatneau offerable.
 * La regle "deux creneaux se chevauchent" est ici (pas dans un service) :
 * c'est une verite du domaine, reutilisable par le booking et la reschedule.
 */
public record Slot(LocalDateTime start, int durationMinutes) {

    public LocalDateTime end() {
        return start.plusMinutes(durationMinutes);
    }

    public boolean overlaps(Slot other) {
        return start.isBefore(other.end()) && other.start.isBefore(end());
    }
}
