package com.prdv.events;

import com.prdv.shared.event.DomainEvent;
import com.prdv.shared.event.DomainEventHandler;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Aiguillage OBSERVER : diffuse chaque evenement de domaine a tous les handlers
 * enregistres (notifications, liste d'attente, audit...). Ajouter un consommateur
 * = declarer un nouveau DomainEventHandler bean, sans toucher a cette classe.
 */
@Component
public class DomainEventRouter {

    private final List<DomainEventHandler> handlers;

    public DomainEventRouter(List<DomainEventHandler> handlers) {
        this.handlers = handlers;
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        for (DomainEventHandler handler : handlers) {
            if (handler.supports(event)) {
                handler.handle(event);
            }
        }
    }
}
