package com.prdv.rdv.profile.adapter.out.iam;

import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapteur anti-corruption : expose l'utilisateur authentifie (detenu par le
 * contexte IAM) au contexte profils.
 *
 * <p>Le contexte profils depend de son propre port ; si le module 1 evolue,
 * seul cet adapteur change (Dependency Inversion, isolation des contextes).
 */
@Component
public class IamCurrentUserAdapter implements CurrentUserPort {

    private final SecurityContextPort securityContext;

    public IamCurrentUserAdapter(SecurityContextPort securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public Optional<Long> currentUserId() {
        return securityContext.currentUserId();
    }

    @Override
    public Long requireCurrentUserId() {
        return securityContext.requireCurrentUserId();
    }
}
