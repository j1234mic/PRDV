package com.prdv.rdv.common.domain;

/**
 * Exception de base du domaine metier : toute violation d'une regle metier
 * herite de cette classe (cf. Dependency Rule de l'architecture hexagonale :
 * le domaine ne depend d'aucun framework).
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
