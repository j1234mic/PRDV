package com.prdv.rdv.iam.application.port.output;

import java.util.Optional;

/**
 * Abstraction du contexte de securite : la couche application ne depend ni de
 * Spring Security ni du HTTP. Permet de connaitre l'utilisateur connecte.
 */
public interface SecurityContextPort {

    Optional<Long> currentUserId();

    Long requireCurrentUserId();
}
