package com.prdv.rdv.iam.application.port.input;

import java.util.Set;

/**
 * Resolution des autorites effectives d'un utilisateur :
 * permissions des roles + permissions directes + delegations temporaires actives.
 * Appele a chaque requete par le filtre JWT : la revocation est instantanee.
 */
public interface AuthorizationQueryUseCase {

    Set<String> authoritiesFor(Long userId);
}
