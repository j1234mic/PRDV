package com.prdv.adapters.out.messaging;

import com.prdv.identity.application.port.out.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Mode dev/demo : le code OTP est loggue (jamais en production). */
@Component
@ConditionalOnProperty(prefix = "prdv.integrations", name = "transport", havingValue = "log", matchIfMissing = true)
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void sendOtp(String destinationEmail, String code) {
        log.info("[OTP][transport=log] email={} code={} (a retrouver dans les logs pour la demo)",
                destinationEmail, code);
    }
}
