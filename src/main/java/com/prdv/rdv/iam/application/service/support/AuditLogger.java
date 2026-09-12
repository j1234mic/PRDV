package com.prdv.rdv.iam.application.service.support;

import com.prdv.rdv.iam.application.port.output.AuditLogRepository;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Service applicatif utilitaire consignant les evenements d'audit de maniere
 * homogene (audit trail / historique des actions).
 */
@Component
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditLogger(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    public void record(Long userId, AuditLog.Action action, AuditLog.Outcome outcome,
                       String resourceType, String resourceId, String detail, String ipAddress) {
        auditLogRepository.save(AuditLog.record(userId, action, outcome,
                resourceType, resourceId, detail, ipAddress, clock));
    }

    public void success(Long userId, AuditLog.Action action, String detail) {
        record(userId, action, AuditLog.Outcome.SUCCESS, null, null, detail, null);
    }

    public void failure(Long userId, AuditLog.Action action, String detail, String ipAddress) {
        record(userId, action, AuditLog.Outcome.FAILURE, null, null, detail, ipAddress);
    }
}
