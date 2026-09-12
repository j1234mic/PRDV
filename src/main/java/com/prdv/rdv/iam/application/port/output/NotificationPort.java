package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;

/**
 * Sortie des notifications (SMS / email / push).
 * Les adapteurs concrets : fournisseur SMS, SMTP, SendGrid...
 * En dev, un adapteur journalisant (Logback) remplace les canaux reels.
 */
public interface NotificationPort {

    void sendOtp(String target, OtpChallenge.Channel channel, String code, OtpChallenge.Purpose purpose);

    void send(String email, String subject, String body);
}
