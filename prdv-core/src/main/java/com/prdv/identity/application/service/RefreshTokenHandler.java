package com.prdv.identity.application.service;

import com.prdv.identity.application.model.AuthTokens;
import com.prdv.identity.application.port.in.RefreshTokenUseCase;
import com.prdv.identity.application.port.out.TokenIssuer;
import com.prdv.identity.application.port.out.TokenVerifier;
import com.prdv.identity.domain.model.TokenType;
import com.prdv.identity.domain.model.User;
import com.prdv.shared.exception.DomainException;
import com.prdv.shared.exception.NotFoundException;

import java.time.Clock;
import java.time.LocalDateTime;

/** Rotation des jetons : le refresh token est change contre une nouvelle paire. */
public final class RefreshTokenHandler implements RefreshTokenUseCase {

    private final TokenVerifier tokenVerifier;
    private final TokenIssuer tokenIssuer;
    private final com.prdv.identity.application.port.out.UserRepository users;
    private final Clock clock;

    public RefreshTokenHandler(TokenVerifier tokenVerifier, TokenIssuer tokenIssuer,
                               com.prdv.identity.application.port.out.UserRepository users, Clock clock) {
        this.tokenVerifier = tokenVerifier;
        this.tokenIssuer = tokenIssuer;
        this.users = users;
        this.clock = clock;
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        TokenVerifier.Payload payload = tokenVerifier.verify(refreshToken, TokenType.REFRESH)
                .orElseThrow(() -> new DomainException("Refresh token invalide ou expire"));
        User user = users.findById(payload.userId()).orElseThrow(() -> new NotFoundException("Compte introuvable"));
        user.ensureCanAuthenticate(LocalDateTime.now(clock));
        return tokenIssuer.issue(user);
    }
}
