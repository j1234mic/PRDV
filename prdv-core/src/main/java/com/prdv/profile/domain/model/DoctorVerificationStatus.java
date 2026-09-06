package com.prdv.profile.domain.model;

/** Workflow de validation manuelle du praticien (module 1.1 "validation manuelle" + 13.3 ordre). */
public enum DoctorVerificationStatus {
    NOT_SUBMITTED,
    PENDING_VALIDATION,
    VERIFIED,
    REJECTED
}
