package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.event.DomainEvent;

/**
 * Publication des evenements de domaine. L'adapteur Spring utilise
 * {@link org.springframework.context.ApplicationEventPublisher} (pattern Observer),
 * permettant d'ajouter des abonnes (emails, analytics, SIEM) sans modifier
 * les cas d'usage (Open/Closed).
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
