package com.prdv.shared.exception;

/** Donnnees d'entree invalides (400 cote REST). */
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }
}
