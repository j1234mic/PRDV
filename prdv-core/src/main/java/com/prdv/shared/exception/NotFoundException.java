package com.prdv.shared.exception;

/** Ressource inexistante (404 cote REST). */
public class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super(message);
    }
}
