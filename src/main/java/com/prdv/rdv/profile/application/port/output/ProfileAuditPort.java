package com.prdv.rdv.profile.application.port.output;

/**
 * Port de sortie : journal d'audit transverse (trace des acces aux donnees
 * de sante, des partages et des consentements).
 *
 * <p>L'adapteur alimente le journal d'audit unique du module IAM, ce qui
 * garantit une piste d'audit consolidée pour les administrateurs.
 */
public interface ProfileAuditPort {

    void record(ProfileAuditAction action, Long userId, String resourceType, String resourceId,
                String detail, boolean success);

    enum ProfileAuditAction {
        PATIENT_PROFILE_UPDATED,
        MEDICAL_RECORD_UPDATED,
        MEDICAL_RECORD_ACCESSED,
        MEDICAL_DOCUMENT_UPLOADED,
        MEDICAL_DOCUMENT_DOWNLOADED,
        MEDICAL_DOCUMENT_SHARED,
        CONSENT_RECORDED,
        VISIBILITY_UPDATED,
        DATA_EXPORTED,
        DATA_ERASURE_REQUESTED,
        DEVICE_CONNECTED,
        HEALTH_ALERT_TRIGGERED,
        PRACTITIONER_PROFILE_UPDATED,
        PRACTITIONER_RATED,
        PRACTITIONER_BADGES_REFRESHED
    }
}
