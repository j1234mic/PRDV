package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Authentification biometrique (empreinte digitale Touch ID, reconnaissance
 * faciale Face ID, cle Passkey WebAuthn / FIDO2).
 */
public interface BiometricAuthenticationUseCase {

    Views.BiometricCredentialView enroll(AuthCommands.EnrollBiometric command);

    List<Views.BiometricCredentialView> listMyCredentials();

    void revoke(Long credentialId);

    AuthResults.Challenge issueChallenge(AuthCommands.RequestBiometricChallenge command);

    AuthResults.AuthResult authenticate(AuthCommands.VerifyBiometric command);
}
