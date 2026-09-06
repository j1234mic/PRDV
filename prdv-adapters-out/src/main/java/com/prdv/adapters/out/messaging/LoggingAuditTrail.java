package com.prdv.adapters.out.messaging;

import com.prdv.notification.application.port.out.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Audit trail (module 8.2 "Logs d'acces detailles"). Adaptateur simple ; la version
 * production ecrirait dans une table append-only (ou un SIEM) via le MEME port.
 */
@Component
public class LoggingAuditTrail implements AuditLog {

    private static final Logger log = LoggerFactory.getLogger("PRDV-AUDIT");

    @Override
    public void record(String action, String detail) {
        log.info("[AUDIT] {} :: {}", action, detail);
    }
}
