package com.prdv.rdv.profile.application.port.output;

import java.util.Optional;

/**
 * Port de sortie : identite de l'utilisateur authentifie.
 *
 * <p>Le contexte « profils » ne depend ni de Spring Security ni du contexte
 * IAM : l'adapteur ({@code IamCurrentUserAdapter}) fait le pont avec le port
 * de securite du module 1 (couche anti-corruption entre contextes).
 */
public interface CurrentUserPort {

    Optional<Long> currentUserId();

    Long requireCurrentUserId();
}
