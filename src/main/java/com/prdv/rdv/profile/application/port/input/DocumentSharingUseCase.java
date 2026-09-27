package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.DocumentCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

import java.util.List;

/** Cas d'usage : partage securise de documents avec des praticiens et avec le DMP. */
public interface DocumentSharingUseCase {

    ProfileViews.DocumentShareView share(DocumentCommands.ShareDocument command);

    ProfileViews.DocumentShareView revokeShare(Long documentId, String shareId);

    List<ProfileViews.DocumentShareView> sharesOf(Long documentId);

    /** Transmission au DMP national via la passerelle. */
    ProfileViews.MedicalDocumentView pushToDmp(DocumentCommands.PushToDmp command);
}
