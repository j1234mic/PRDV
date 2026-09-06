package com.prdv.identity.application.port.in;

import com.prdv.identity.application.model.AuthTokens;

public interface LoginUseCase {

    record Command(String email, String password) { }

    AuthTokens login(Command command);
}
