package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Inscription praticien : verification RPPS/ADELI, diplomes, RIB, assurance,
 * contrat electronique, rattachement multi-etablissements et remplacements.
 */
public interface PractitionerRegistrationUseCase {

    Views.OtpSentView register(RegistrationCommands.RegisterPractitioner command);

    Views.PractitionerView currentProfile();

    Views.KycDocumentView uploadDocument(ProfileCommands.UploadKycDocument command);

    List<Views.KycDocumentView> myDocuments();

    Views.ContractView acceptContract(ProfileCommands.AcceptContract command);

    /** Le praticien demande son rattachement a un cabinet (ou un remplacement). */
    Views.MembershipView requestMembership(ProfileCommands.RequestMembership command);

    Views.MembershipView declareReplacement(ProfileCommands.DeclareReplacement command);

    List<Views.MembershipView> myMemberships();
}
