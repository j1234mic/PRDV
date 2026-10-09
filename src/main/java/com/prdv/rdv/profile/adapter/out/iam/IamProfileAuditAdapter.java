package com.prdv.rdv.profile.adapter.out.iam;

import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import org.springframework.stereotype.Component;

/**
 * Adapteur : alimente le journal d'audit unique du contexte IAM depuis le
 * contexte profils.
 *
 * <p>Les administrateurs disposent ainsi d'une piste d'audit consolidée
 * (connexions, roles, KYC, acces aux donnees de sante, partages,
 * consentements) au lieu de journaux paralleles.
 */
@Component
public class IamProfileAuditAdapter implements ProfileAuditPort {

    private final AuditLogger auditLogger;

    public IamProfileAuditAdapter(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void record(ProfileAuditAction action, Long userId, String resourceType, String resourceId,
                       String detail, boolean success) {
        auditLogger.record(userId, toIamAction(action),
                success ? AuditLog.Outcome.SUCCESS : AuditLog.Outcome.FAILURE,
                resourceType, resourceId, detail, null);
    }

    /** Traduction vers le catalogue d'actions du journal d'audit transverse. */
    private AuditLog.Action toIamAction(ProfileAuditAction action) {
        return switch (action) {
            case PATIENT_PROFILE_UPDATED -> AuditLog.Action.PROFILE_UPDATED;
            case MEDICAL_RECORD_UPDATED -> AuditLog.Action.MEDICAL_RECORD_UPDATED;
            case MEDICAL_RECORD_ACCESSED -> AuditLog.Action.MEDICAL_RECORD_ACCESSED;
            case MEDICAL_DOCUMENT_UPLOADED -> AuditLog.Action.MEDICAL_DOCUMENT_UPLOADED;
            case MEDICAL_DOCUMENT_DOWNLOADED -> AuditLog.Action.MEDICAL_DOCUMENT_DOWNLOADED;
            // Un fichier d'identite ou multimedia est trace comme un telechargement de document.
            case MEDIA_DOWNLOADED -> AuditLog.Action.MEDICAL_DOCUMENT_DOWNLOADED;
            case MEDICAL_DOCUMENT_SHARED -> AuditLog.Action.MEDICAL_DOCUMENT_SHARED;
            case CONSENT_RECORDED -> AuditLog.Action.CONSENT_RECORDED;
            case VISIBILITY_UPDATED -> AuditLog.Action.VISIBILITY_UPDATED;
            case DATA_EXPORTED -> AuditLog.Action.DATA_EXPORTED;
            case DATA_ERASURE_REQUESTED -> AuditLog.Action.DATA_ERASURE_REQUESTED;
            case DEVICE_CONNECTED -> AuditLog.Action.CONNECTED_DEVICE_MANAGED;
            case HEALTH_ALERT_TRIGGERED -> AuditLog.Action.HEALTH_ALERT_TRIGGERED;
            case PRACTITIONER_PROFILE_UPDATED -> AuditLog.Action.PRACTITIONER_PROFILE_UPDATED;
            case PRACTITIONER_RATED -> AuditLog.Action.PRACTITIONER_RATED;
            case PRACTITIONER_BADGES_REFRESHED -> AuditLog.Action.PRACTITIONER_BADGES_REFRESHED;
        };
    }
}
