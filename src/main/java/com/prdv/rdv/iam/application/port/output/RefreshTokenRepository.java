package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.RefreshToken;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    /** Recherche y compris les jetons revoques : indispensable pour detecter la reutilisation frauduleuse. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByFamily(String family);

    List<RefreshToken> findByUserId(Long userId);
}
