package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.SocialAccount;

import java.util.Optional;

public interface SocialAccountRepository {

    SocialAccount save(SocialAccount account);

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialAccount.Provider provider,
                                                            String providerUserId);

    Optional<SocialAccount> findByUserIdAndProvider(Long userId, SocialAccount.Provider provider);
}
