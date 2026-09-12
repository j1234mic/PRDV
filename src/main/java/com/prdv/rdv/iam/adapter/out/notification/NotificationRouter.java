package com.prdv.rdv.iam.adapter.out.notification;

import com.prdv.rdv.iam.application.port.output.NotificationPort;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Routeur de notification : envoie par SMTP quand le serveur mail est configure
 * (variable {@code spring.mail.host}), sinon bascule sur l'adapteur de journal.
 *
 * <p>Les SMS emprunteront le meme mecanisme (ajout d'un adapteur Twilio/Orange :
 * aucun changement cote domaine).
 */
@Primary
@Component
public class NotificationRouter implements NotificationPort {

    private final LoggingNotificationAdapter fallback;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public NotificationRouter(LoggingNotificationAdapter fallback,
                              ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.fallback = fallback;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void sendOtp(String target, OtpChallenge.Channel channel, String code, OtpChallenge.Purpose purpose) {
        if (channel == OtpChallenge.Channel.EMAIL) {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender != null) {
                send(target, "Votre code de verification PRDV",
                        "Code (usage " + purpose + ") : " + code + ". Valable 5 minutes.");
                return;
            }
        }
        fallback.sendOtp(target, channel, code, purpose);
    }

    @Override
    public void send(String email, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null && email != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@prdv.app");
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } else {
            fallback.send(email, subject, body);
        }
    }
}
