package com.prdv.shared.exception;

/** L'appelant n'est pas proprietaire de la ressource (403 cote REST). */
public class ForbiddenOperationException extends DomainException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
