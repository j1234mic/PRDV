package com.prdv.rdv.iam.domain.model.audit;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Entree d'audit (audit trail) : toute action sensible est journalisee de
 * maniere immuable (connexions, changements de droits, validations KYC...).
 * Repond au besoin « historique des actions » et aux logs d'acces detailles.
 */
@Getter
@Setter
public class AuditLog {

    public enum Action {
        USER_REGISTERED,
        OTP_REQUESTED,
        OTP_VERIFIED,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGIN_SUSPICIOUS,
        TOKEN_REFRESHED,
        LOGOUT,
        MFA_ENABLED,
        MFA_DISABLED,
        PASSWORD_CHANGED,
        ACCOUNT_LOCKED,
        SESSION_REVOKED,
        ROLE_ASSIGNED,
        ROLE_CREATED,
        DELEGATION_GRANTED,
        DELEGATION_REVOKED,
        KYC_DOCUMENT_UPLOADED,
        KYC_DOCUMENT_REVIEWED,
        PRACTITIONER_VALIDATED,
        ESTABLISHMENT_VALIDATED,
        ACCOUNT_SUSPENDED,
        ACCOUNT_ACTIVATED,
        ACCOUNT_ANONYMIZED,
        SOCIAL_LOGIN
    }

    public enum Outcome { SUCCESS, FAILURE }

    private Long id;
    private Long userId;
    private Action action;
    private Outcome outcome;
    private String resourceType;
    private String resourceId;
    private String detail;
    private String ipAddress;
    private Instant createdAt;

    public static AuditLog record(Long userId, Action action, Outcome outcome,
                                  String resourceType, String resourceId, String detail,
                                  String ipAddress, Clock clock) {
        AuditLog log = new AuditLog();
        log.userId = userId;
        log.action = action;
        log.outcome = outcome;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.detail = detail;
        log.ipAddress = ipAddress;
        log.createdAt = clock.instant();
        return log;
    }
}
