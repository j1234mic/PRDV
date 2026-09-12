package com.prdv.rdv.iam.domain.exception;

import com.prdv.rdv.common.domain.DomainException;

/**
 * Unique exception fonctionnelle du module IAM, identifiee par un {@link IamErrorCode}.
 * Le choix d'une exception codee (plutot qu'une vingtaine de classes) facilite
 * la traduction centrale dans la couche web et l'internationalisation des messages.
 */
public class IamException extends DomainException {

    private final transient IamErrorCode errorCode;

    public IamException(IamErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static IamException of(IamErrorCode code, String message) {
        return new IamException(code, message);
    }

    public IamErrorCode getErrorCode() {
        return errorCode;
    }
}
