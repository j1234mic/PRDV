package com.prdv.rdv.profile.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Catalogue des erreurs fonctionnelles du module 2 (profils &amp; donnees).
 *
 * <p>Comme pour le module IAM, chaque code porte la semantique HTTP associee :
 * le domaine n'embarque aucune dependance web (pas de servlet, pas de Spring
 * MVC), il se contente d'exprimer l'intention de l'erreur. La traduction
 * effective en reponse HTTP est realisee par
 * {@code com.prdv.rdv.common.web.GlobalExceptionHandler}.
 */
public enum ProfileErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    BIOMETRIC_OUT_OF_RANGE(HttpStatus.BAD_REQUEST),
    DOCUMENT_FORMAT_UNSUPPORTED(HttpStatus.BAD_REQUEST),
    DOCUMENT_TOO_LARGE(HttpStatus.BAD_REQUEST),
    DOCUMENT_IMMUTABLE(HttpStatus.BAD_REQUEST),
    SHARE_ALREADY_REVOKED(HttpStatus.BAD_REQUEST),
    CONSENT_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST),
    RATING_OUT_OF_RANGE(HttpStatus.BAD_REQUEST),
    LOCATION_INVALID(HttpStatus.BAD_REQUEST),

    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND),

    CONSENT_REQUIRED(HttpStatus.FORBIDDEN),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    PRACTITIONER_PROFILE_INCOMPLETE(HttpStatus.FORBIDDEN),

    RATING_ALREADY_SUBMITTED(HttpStatus.CONFLICT),
    DEVICE_ALREADY_CONNECTED(HttpStatus.CONFLICT),

    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY),
    STORAGE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ProfileErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus.value();
    }
}
