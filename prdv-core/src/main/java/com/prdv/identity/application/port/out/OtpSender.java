package com.prdv.identity.application.port.out;

/**
 * Port de sortie - pattern ADAPTER en face : aujourd'hui un adaptateur "log" (dev),
 * demain SendGrid / Twilio sans changer le coeur (Open/Closed).
 */
public interface OtpSender {
    void sendOtp(String destinationEmail, String code);
}
