package com.prdv.events;

import com.prdv.shared.event.DomainEvent;
import com.prdv.shared.event.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Adaptateur MINCE du port DomainEventPublisher vers Spring.
 * Synchrone par defaut : les listeners s'executent dans la MEME transaction que le
 * booking (atomicite annulation -> re-attribution liste d'attente garantie).
 * A terme (perf) : passer a un @TransactionalEventListener + outbox table.
 */
public final class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    public SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
