package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.SocialAccountJpaRepository;
import com.prdv.rdv.iam.application.port.output.SocialAccountRepository;
import com.prdv.rdv.iam.domain.model.auth.SocialAccount;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapteur JPA des comptes sociaux (Google, Keycloak, Azure AD, Okta...).
 */
@Repository
public class SocialAccountPersistenceAdapter implements SocialAccountRepository {

    private final SocialAccountJpaRepository socialJpa;
    private final AuthPersistenceMapper mapper;

    public SocialAccountPersistenceAdapter(SocialAccountJpaRepository socialJpa,
                                           AuthPersistenceMapper mapper) {
        this.socialJpa = socialJpa;
        this.mapper = mapper;
    }

    @Override
    public SocialAccount save(SocialAccount account) {
        return mapper.toDomain(socialJpa.save(mapper.toEntity(account)));
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderUserId(SocialAccount.Provider provider,
                                                                   String providerUserId) {
        return socialJpa.findByProviderAndProviderUserId(provider, providerUserId).map(mapper::toDomain);
    }

    @Override
    public Optional<SocialAccount> findByUserIdAndProvider(Long userId, SocialAccount.Provider provider) {
        return socialJpa.findByUserIdAndProvider(userId, provider).map(mapper::toDomain);
    }
}
