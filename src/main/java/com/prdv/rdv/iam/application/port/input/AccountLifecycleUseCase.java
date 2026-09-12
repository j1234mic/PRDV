package com.prdv.rdv.iam.application.port.input;

/** Droit a l'oubli : anonymisation / suppression de son compte (RGPD). */
public interface AccountLifecycleUseCase {

    void deleteMyAccount();
}
