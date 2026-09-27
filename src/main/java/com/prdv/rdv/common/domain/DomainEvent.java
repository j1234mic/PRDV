package com.prdv.rdv.common.domain;

import java.time.Instant;

/**
 * Marqueur commun a tous les evenements de domaine de la plateforme
 * (kernel partage entre contextes delimites : IAM, profils, rendez-vous...).
 *
 * <p>Un evenement est un fait passe, immuable et horodate. Le domaine les
 * emet sans connaitre ses abonnes (pattern Observer) : notifications,
 * analytics, SIEM, cache... peuvent s'y brancher sans modifier les cas
 * d'usage (Open/Closed).
 */
public interface DomainEvent {

    /** Instant auquel le fait s'est produit. */
    Instant occurredAt();
}
