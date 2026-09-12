package com.prdv.rdv.iam.adapter.out.notification;

import com.prdv.rdv.iam.application.port.output.NotificationPort;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapteur de notification pour le developpement : journalise les OTP
 * (aucun SMS / email reel). {@link NotificationRouter} s'en sert de repli.
 */
@Component
public class LoggingNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    @Override
    public void sendOtp(String target, OtpChallenge.Channel channel, String code, OtpChallenge.Purpose purpose) {
        log.info("[NOTIFICATION {}] Code OTP pour {} (usage {}) : {}",
                channel, target, purpose, code);
    }

    @Override
    public void send(String email, String subject, String body) {
        log.info("[NOTIFICATION EMAIL] A : {} | Objet : {} | Corps : {}", email, subject, body);
    }
}
