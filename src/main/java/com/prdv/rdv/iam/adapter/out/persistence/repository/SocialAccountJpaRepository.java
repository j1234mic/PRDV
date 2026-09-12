package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.SocialAccountEntity;
import com.prdv.rdv.iam.domain.model.auth.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountJpaRepository extends JpaRepository<SocialAccountEntity, Long> {

    Optional<SocialAccountEntity> findByProviderAndProviderUserId(
            SocialAccount.Provider provider, String providerUserId);

    Optional<SocialAccountEntity> findByUserIdAndProvider(Long userId, SocialAccount.Provider provider);
}
