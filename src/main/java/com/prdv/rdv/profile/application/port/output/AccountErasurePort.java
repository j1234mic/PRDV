package com.prdv.rdv.profile.application.port.output;

/**
 * Port de sortie : demande d'anonymisation du compte utilisateur aupres du
 * contexte IAM (droit a l'oubli). Le contexte profils purge ses propres
 * donnees puis delegate l'anonymisation du compte, sans dependre de ses
 * classes internes.
 */
public interface AccountErasurePort {

    void requestAccountAnonymization(Long userId);
}
