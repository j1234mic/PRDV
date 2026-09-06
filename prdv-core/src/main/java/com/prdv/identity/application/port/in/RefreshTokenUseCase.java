package com.prdv.identity.application.port.in;

import com.prdv.identity.application.model.AuthTokens;

public interface RefreshTokenUseCase {

    AuthTokens refresh(String refreshToken);
}
