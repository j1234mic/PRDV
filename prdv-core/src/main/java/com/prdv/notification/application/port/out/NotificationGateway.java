package com.prdv.notification.application.port.out;

import com.prdv.notification.domain.model.Notification;

/**
 * Port de sortie (SECONDARY PORT) multi-canal. Adaptateurs :
 *  - LoggingNotificationGateway (dev/demo)
 *  - EmailNotificationGateway (SMTP)   |  selection par config -> Liskov + Open/Closed.
 */
public interface NotificationGateway {
    void send(Notification notification);
}
