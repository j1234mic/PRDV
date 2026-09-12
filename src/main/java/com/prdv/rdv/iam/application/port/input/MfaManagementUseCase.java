package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.result.AuthResults;

/**
 * Gestion de l'authentification multifacteur TOTP
 * (Google Authenticator, compatible empreinte / Face ID cote terminal).
 */
public interface MfaManagementUseCase {

    /** Genere le secret et l'URI otpauth:// (QR code) avant activation. */
    AuthResults.MfaSetup beginSetup();

    /** Confirme le premier code TOTP puis active la MFA. */
    void enable(String code);

    void disable();
}
