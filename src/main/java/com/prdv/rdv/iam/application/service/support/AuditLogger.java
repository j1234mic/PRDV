package com.prdv.rdv.iam.application.service.support;

import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Service applicatif utilitaire consignant les evenements d'audit de maniere
 * homogene (audit trail / historique des actions).
 *
 * <p>Les journaux d'audit sont persistes dans une transaction independante
 * ({@link TransactionalStateSaver#saveAuditLog}) afin de survivre a un
 * eventuel rollback de la transaction metier appelante. Un evenement
 * d'audit represente un fait constate et ne doit jamais etre victime d'un
 * rollback fonctionnel.</p>
 */
@Component
public class AuditLogger {

    private final TransactionalStateSaver stateSaver;
    private final Clock clock;

    public AuditLogger(TransactionalStateSaver stateSaver, Clock clock) {
        this.stateSaver = stateSaver;
        this.clock = clock;
    }

    public void record(Long userId, AuditLog.Action action, AuditLog.Outcome outcome,
                       String resourceType, String resourceId, String detail, String ipAddress) {
        stateSaver.saveAuditLog(AuditLog.record(userId, action, outcome,
                resourceType, resourceId, detail, ipAddress, clock));
    }

    public void success(Long userId, AuditLog.Action action, String detail) {
        record(userId, action, AuditLog.Outcome.SUCCESS, null, null, detail, null);
    }

    public void failure(Long userId, AuditLog.Action action, String detail, String ipAddress) {
        record(userId, action, AuditLog.Outcome.FAILURE, null, null, detail, ipAddress);
    }
}
