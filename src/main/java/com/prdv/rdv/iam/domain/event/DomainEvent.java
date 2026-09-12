package com.prdv.rdv.iam.domain.event;

import java.time.Instant;

/**
 * Evenement de domaine (pattern Observer / integration events).
 * Le domaine emet des evenements sans connaitre les abonnes :
 * notifications, emails aux administrateurs, analytics, etc.
 */
public interface DomainEvent {

    Instant occurredAt();

    /** Inscription d'un nouveau patient. */
    record PatientRegistered(Long userId, String email, Instant occurredAt) implements DomainEvent {
    }

    /** Dossier praticien soumis, en attente de validation manuelle. */
    record PractitionerApplicationSubmitted(Long userId, String email, String rpps, Instant occurredAt)
            implements DomainEvent {
    }

    /** Compte etablissement soumis pour validation. */
    record EstablishmentApplicationSubmitted(Long userId, String legalName, Instant occurredAt)
            implements DomainEvent {
    }

    /** OTP genere (l'adapter de notification envoie email/SMS). */
    record OtpGenerated(String target, Object channel, Object purpose, Instant occurredAt)
            implements DomainEvent {
    }

    /** Connexion suspecte detectee (le module de fraude demande une verification). */
    record SuspiciousLoginDetected(Long userId, String reason, String ipAddress, Instant occurredAt)
            implements DomainEvent {
    }

    /** Praticien valide / refuse par un moderateur. */
    record PractitionerReviewed(Long userId, boolean approved, String reason, Instant occurredAt)
            implements DomainEvent {
    }
}
