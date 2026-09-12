package com.prdv.rdv.iam.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Catalogue des erreurs fonctionnelles du module IAM.
 * Chaque code porte le statut HTTP associe : le domaine reste independant
 * du web, mais indique la semantique de l'erreur.
 */
public enum IamErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST),
    PROFILE_MISMATCH(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_PROFILE(HttpStatus.BAD_REQUEST),
    DELEGATION_INVALID(HttpStatus.BAD_REQUEST),
    IMPORT_FORMAT_INVALID(HttpStatus.UNPROCESSABLE_ENTITY),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    APPLICATION_ALREADY_REVIEWED(HttpStatus.CONFLICT),
    CONTRACT_NOT_ACCEPTED(HttpStatus.CONFLICT),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    MFA_INVALID_CODE(HttpStatus.UNAUTHORIZED),
    OTP_INVALID(HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST),
    OTP_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED),

    ACCOUNT_LOCKED(HttpStatus.LOCKED),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    FORBIDDEN(HttpStatus.FORBIDDEN),

    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY);

    private final HttpStatus httpStatus;

    IamErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus.value();
    }
}
