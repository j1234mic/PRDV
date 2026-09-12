package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.result.Views;

/**
 * Inscription et parcours patient (OTP, profil mineur rattache, KYC,
 * import de donnees depuis une autre plateforme).
 */
public interface PatientRegistrationUseCase {

    /** Cree le compte et declenche la verification OTP (email/SMS). */
    Views.OtpSentView register(RegistrationCommands.RegisterPatient command);

    /** Depot d'une piece d'identite (CNI / passeport) pour le KYC paiement. */
    Views.KycDocumentView uploadIdentityDocument(ProfileCommands.UploadKycDocument command);
}
