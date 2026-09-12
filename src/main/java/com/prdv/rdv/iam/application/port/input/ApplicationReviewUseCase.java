package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Validation manuelle des inscriptions (praticiens et etablissements)
 * par les moderateurs, et revue des documents KYC.
 */
public interface ApplicationReviewUseCase {

    List<Views.PractitionerView> pendingPractitioners();

    List<Views.EstablishmentView> pendingEstablishments();

    /** Approuve ou rejette le dossier d'un praticien / etablissement. */
    Views.UserView reviewApplication(AdminCommands.ReviewApplication command);

    Views.PagedResult<Views.KycDocumentView> pendingDocuments(int page, int size);

    Views.KycDocumentView reviewDocument(AdminCommands.ReviewDocument command);
}
