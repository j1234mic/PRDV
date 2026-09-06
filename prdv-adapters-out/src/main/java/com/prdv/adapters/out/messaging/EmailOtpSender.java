package com.prdv.adapters.out.messaging;

import com.prdv.identity.application.port.out.OtpSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Mode reel : SendGrid/SMTP via spring-boot-starter-mail (module 14 "Communication"). */
@Component
@ConditionalOnProperty(prefix = "prdv.integrations", name = "transport", havingValue = "mail")
public class EmailOtpSender implements OtpSender {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailOtpSender(JavaMailSender mailSender,
                          @Value("${spring.mail.username:no-reply@prdv.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendOtp(String destinationEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(destinationEmail);
        message.setSubject("[PRDV] Votre code de verification");
        message.setText("Votre code de verification est : " + code
                + "\nIl expire dans 10 minutes. Ne le partagez avec personne.");
        mailSender.send(message);
    }
}
