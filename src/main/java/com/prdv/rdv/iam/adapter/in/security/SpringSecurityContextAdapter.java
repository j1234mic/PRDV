package com.prdv.rdv.iam.adapter.in.security;

import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapteur du contexte Spring Security vers le port de l'application.
 */
@Component
public class SpringSecurityContextAdapter implements SecurityContextPort {

    @Override
    public Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user.userId());
        }
        return Optional.empty();
    }

    @Override
    public Long requireCurrentUserId() {
        return currentUserId()
                .orElseThrow(() -> IamException.of(IamErrorCode.FORBIDDEN, "Utilisateur non authentifie"));
    }
}
