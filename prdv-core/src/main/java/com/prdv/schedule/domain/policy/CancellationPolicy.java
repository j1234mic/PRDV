package com.prdv.schedule.domain.policy;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Pattern STRATEGY (module 4.2) : "Delai minimum annulation sans frais (24h, 48h)",
 * "Penalites annulation tardive". Une regle = une petite classe, interchangeable,
 * testable seule. Le domaine RDV ne fait QUE poser la question.
 */
public interface CancellationPolicy {

    /** Vrai si l'annulation est tardive (dans la fenetre penalisable avant le RDV). */
    boolean isLate(LocalDateTime slotStart, LocalDateTime now);

    /** Penalite forfaitaire en centimes (partageee avec la plateforme en regle). */
    int cancellationFeeCents();

    /** Un deplacement est permis tant qu'il n'est pas dans la fenetre d'annulation tardive. */
    default boolean isRescheduleAllowed(LocalDateTime slotStart, LocalDateTime now) {
        return !isLate(slotStart, now) && now.isBefore(slotStart.minus(Duration.ofHours(1)));
    }
}
