package com.prdv.rdv.profile.adapter.out.iam;

import com.prdv.rdv.iam.application.port.input.AccountLifecycleUseCase;
import com.prdv.rdv.profile.application.port.output.AccountErasurePort;
import org.springframework.stereotype.Component;

/**
 * Adapteur : apres purge de ses propres donnees, le contexte profils demande
 * au contexte IAM l'anonymisation irreversible du compte (droit a l'oubli).
 */
@Component
public class IamAccountErasureAdapter implements AccountErasurePort {

    private final AccountLifecycleUseCase accountLifecycle;

    public IamAccountErasureAdapter(AccountLifecycleUseCase accountLifecycle) {
        this.accountLifecycle = accountLifecycle;
    }

    @Override
    public void requestAccountAnonymization(Long userId) {
        // Le cas d'usage IAM resolve lui-meme l'utilisateur authentifie.
        accountLifecycle.deleteMyAccount();
    }
}
