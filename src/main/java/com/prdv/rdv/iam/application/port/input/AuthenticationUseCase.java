package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.result.AuthResults;

/**
 * Authentification : connexion mot de passe, 2e facteur TOTP ou OTP,
 * rotation des refresh tokens, deconnexion.
 */
public interface AuthenticationUseCase {

    AuthResults.AuthResult login(AuthCommands.Login command);

    AuthResults.AuthResult verifyTotp(AuthCommands.VerifyTotp command);

    AuthResults.AuthResult verifyLoginOtp(AuthCommands.VerifyLoginOtp command);

    AuthResults.TokenSet refresh(AuthCommands.Refresh command);

    void logout(AuthCommands.Logout command);
}
