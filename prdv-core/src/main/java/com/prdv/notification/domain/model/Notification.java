package com.prdv.notification.domain.model;

/**
 * Value Object d'une notification. Le corps ne doit JAMAIS contenir de donnee medicale
 * sensible (secret medical + RGPD) : on notifie l'evenement, le contenu se lit dans l'app.
 */
public record Notification(Long recipientUserId, NotificationChannel channel, String subject, String body) {

    public Notification {
        if (recipientUserId == null || channel == null || subject == null || body == null) {
            throw new IllegalArgumentException("Notification incomplete");
        }
    }
}
