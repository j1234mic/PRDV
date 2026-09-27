package com.prdv.rdv.profile.domain.exception;

import com.prdv.rdv.common.domain.DomainException;

/**
 * Exception fonctionnelle du contexte « profils &amp; gestion des donnees ».
 *
 * <p>Une seule classe d'exception portee par un {@link ProfileErrorCode}
 * (comme {@code IamException} cote IAM) : la couche web traduit le code en
 * statut HTTP et les messages restent internationalisables.
 */
public class ProfileException extends DomainException {

    private final transient ProfileErrorCode errorCode;

    public ProfileException(ProfileErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static ProfileException of(ProfileErrorCode code, String message) {
        return new ProfileException(code, message);
    }

    public ProfileErrorCode getErrorCode() {
        return errorCode;
    }
}
