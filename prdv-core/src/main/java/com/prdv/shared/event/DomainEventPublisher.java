package com.prdv.shared.event;

/** Port de sortie (SECONDARY PORT) : comment le coeur publie ses evenements. */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
