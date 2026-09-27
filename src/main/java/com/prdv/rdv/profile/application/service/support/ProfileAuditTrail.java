package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import org.springframework.stereotype.Component;

/**
 * Facade d'audit du contexte profils (Single Responsibility) : les services
 * applicatifs expriment l'intention (« document partage ») sans connaitre le
 * format ni la destination du journal.
 */
@Component
public class ProfileAuditTrail {

    private final ProfileAuditPort auditPort;

    public ProfileAuditTrail(ProfileAuditPort auditPort) {
        this.auditPort = auditPort;
    }

    public void success(ProfileAuditPort.ProfileAuditAction action, Long userId, String resourceType,
                        String resourceId, String detail) {
        auditPort.record(action, userId, resourceType, resourceId, detail, true);
    }

    public void failure(ProfileAuditPort.ProfileAuditAction action, Long userId, String resourceType,
                        String resourceId, String detail) {
        auditPort.record(action, userId, resourceType, resourceId, detail, false);
    }

    public void success(ProfileAuditPort.ProfileAuditAction action, Long userId, String detail) {
        success(action, userId, null, null, detail);
    }
}
