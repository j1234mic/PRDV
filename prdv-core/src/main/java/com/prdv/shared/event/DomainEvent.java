package com.prdv.shared.event;

/**
 * Evenement de domaine (marqueur). Pattern OBSERVER : le coeur emet des evenements
 * via DomainEventPublisher sans connaitre ses consommateurs (notifications, liste
 * d'attente, audit...). Le câblage observateur/sujet est fait dans prdv-app
 * (Spring ApplicationEventPublisher), ce qui garde le coeur sans framework.
 */
public interface DomainEvent {
}
