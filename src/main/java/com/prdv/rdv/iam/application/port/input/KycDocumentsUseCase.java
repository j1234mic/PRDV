package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/** Depot et consultation des documents KYC (partage par les differents profils). */
public interface KycDocumentsUseCase {

    Views.KycDocumentView upload(ProfileCommands.UploadKycDocument command);

    List<Views.KycDocumentView> listForOwner(Long ownerUserId);
}
