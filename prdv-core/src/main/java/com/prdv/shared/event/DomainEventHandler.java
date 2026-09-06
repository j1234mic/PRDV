package com.prdv.shared.event;

/**
 * Port d'entree interne : consommateur d'evenements de domaine.
 * Chaque reacteur (notifications, liste d'attente) est un petit composant
 * independant -> Open/Closed : on ajoute un handler sans modifier les autres.
 */
public interface DomainEventHandler {
    boolean supports(DomainEvent event);
    void handle(DomainEvent event);
}
