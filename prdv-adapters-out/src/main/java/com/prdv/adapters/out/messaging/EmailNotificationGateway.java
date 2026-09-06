package com.prdv.adapters.out.messaging;

import com.prdv.identity.application.port.out.UserRepository;
import com.prdv.notification.application.port.out.NotificationGateway;
import com.prdv.notification.domain.model.Notification;
import com.prdv.notification.domain.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Mode reel EMAIL. Le SMS/Push reste en log tant que Twilio/FCM ne sont pas branches. */
@Component
@ConditionalOnProperty(prefix = "prdv.integrations", name = "transport", havingValue = "mail")
public class EmailNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationGateway.class);

    private final JavaMailSender mailSender;
    private final UserRepository users;
    private final String from;

    public EmailNotificationGateway(JavaMailSender mailSender, UserRepository users,
                                    @Value("${spring.mail.username:no-reply@prdv.local}") String from) {
        this.mailSender = mailSender;
        this.users = users;
        this.from = from;
    }

    @Override
    public void send(Notification notification) {
        if (notification.channel() != NotificationChannel.EMAIL) {
            log.info("[NOTIF canal={} user={}] {} - {} (canal non connecte : trace uniquement)",
                    notification.channel(), notification.recipientUserId(),
                    notification.subject(), notification.body());
            return;
        }
        users.findById(notification.recipientUserId()).ifPresent(user -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(user.email());
            message.setSubject(notification.subject());
            message.setText(notification.body());
            mailSender.send(message);
        });
    }
}
