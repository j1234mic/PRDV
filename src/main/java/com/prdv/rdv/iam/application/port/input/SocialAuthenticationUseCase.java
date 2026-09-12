package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.domain.model.auth.SocialAccount;

/**
 * Connexion / creation automatique via un fournisseur OAuth2-OIDC
 * (Google, Apple, Microsoft, SSO d'entreprise). Appele par le gestionnaire
 * de succes Spring Security apres le flux « reseaux sociaux ».
 */
public interface SocialAuthenticationUseCase {

    AuthResults.TokenSet authenticate(SocialAccount.Provider provider,
                                      String providerUserId,
                                      String email,
                                      String firstName,
                                      String lastName,
                                      RequestMetadata metadata);
}
