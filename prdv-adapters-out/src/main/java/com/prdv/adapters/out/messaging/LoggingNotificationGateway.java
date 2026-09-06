package com.prdv.adapters.out.messaging;

import com.prdv.notification.application.port.out.NotificationGateway;
import com.prdv.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DEMO/DEV : "envoyer" = tracer. L'existence de deux implementations interchangeables
 * du MEME port demontre le principe de substitution (L) : le coeur ne voit aucune difference.
 */
@Component
@ConditionalOnProperty(prefix = "prdv.integrations", name = "transport", havingValue = "log", matchIfMissing = true)
public class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    @Override
    public void send(Notification notification) {
        log.info("[NOTIF user={} canal={}] {} - {}", notification.recipientUserId(),
                notification.channel(), notification.subject(), notification.body());
    }
}
