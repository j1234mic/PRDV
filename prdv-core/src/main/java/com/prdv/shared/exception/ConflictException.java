package com.prdv.shared.exception;

/** Conflit d'etat : creneau deja pris, doublon, action interdite par les regles (409 cote REST). */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
