package com.prdv.rdv.iam.domain.model.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Liaison d'un compte utilisateur avec un fournisseur d'identite externe
 * (Google, Facebook, Apple...) ou un SSO d'entreprise (OIDC).
 * Permet la connexion « reseaux sociaux » et le Single Sign-On.
 */
@Getter
@Setter
public class SocialAccount {

    public enum Provider {
        GOOGLE,
        FACEBOOK,
        APPLE,
        MICROSOFT,
        OIDC_ENTERPRISE
    }

    private Long id;
    private Long userId;
    private Provider provider;
    private String providerUserId;
    private String email;
    private Instant createdAt;

    public static SocialAccount link(Long userId, Provider provider, String providerUserId,
                                     String email, Clock clock) {
        SocialAccount a = new SocialAccount();
        a.userId = userId;
        a.provider = provider;
        a.providerUserId = providerUserId;
        a.email = email;
        a.createdAt = clock.instant();
        return a;
    }
}
