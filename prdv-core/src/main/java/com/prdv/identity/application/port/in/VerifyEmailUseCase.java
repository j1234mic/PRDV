package com.prdv.identity.application.port.in;

import com.prdv.identity.application.model.UserInfo;

public interface VerifyEmailUseCase {

    UserInfo verify(String email, String otpCode);
}
