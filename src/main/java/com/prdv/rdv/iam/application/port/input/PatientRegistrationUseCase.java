package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Inscription et parcours patient (OTP, profil mineur rattache, KYC,
 * import de donnees depuis une autre plateforme).
 */
public interface PatientRegistrationUseCase {

    /** Cree le compte et declenche la verification OTP (email/SMS). */
    Views.OtpSentView register(RegistrationCommands.RegisterPatient command);

    /** Profil complet du patient connecte. */
    Views.PatientView currentProfile();

    /** Inscrit et rattache un profil patient mineur au parent/tuteur connecte. */
    Views.PatientView registerMinor(RegistrationCommands.RegisterMinorPatient command);

    /** Liste les profils mineurs rattaches au parent/tuteur connecte. */
    List<Views.PatientView> listMyMinors();

    /** Depot d'une piece d'identite (CNI / passeport) pour le KYC paiement. */
    Views.KycDocumentView uploadIdentityDocument(ProfileCommands.UploadKycDocument command);

    /** Pieces justificatives KYC deposees par le patient. */
    List<Views.KycDocumentView> myDocuments();

    /** Statut KYC complet et eligibilite aux paiements. */
    Views.PatientKycStatusView myKycStatus();
}
