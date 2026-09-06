package com.prdv.shared.exception;

/**
 * Exception metier generique : regle d'invariant violee (422 cote REST).
 * Le coeur ne connait ni HTTP ni Spring : les adaptateurs traduisent.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
