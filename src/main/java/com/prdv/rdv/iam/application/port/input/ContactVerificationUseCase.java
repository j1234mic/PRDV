package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.result.Views;

/**
 * Confirmation OTP des coordonnees (email / telephone), commune a tous
 * les profils lors de l'inscription, et renvoi de code.
 */
public interface ContactVerificationUseCase {

    /** Verifie le code recu et active le compte (ou le passe en validation manuelle). */
    Views.UserView confirmRegistrationOtp(AuthCommands.VerifyRegistrationOtp command);

    Views.OtpSentView resend(AuthCommands.ResendOtp command);
}
