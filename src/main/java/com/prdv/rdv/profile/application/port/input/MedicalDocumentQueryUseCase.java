package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;

import java.util.List;

/** Cas d'usage : consultation et telechargement des documents medicaux. */
public interface MedicalDocumentQueryUseCase {

    List<ProfileViews.MedicalDocumentView> myDocuments(MedicalDocument.DocumentCategory category);

    /** Documents accessibles au demandeur (propres + partages actifs). */
    List<ProfileViews.MedicalDocumentView> sharedWithMe();

    ProfileViews.MedicalDocumentView document(Long documentId);

    /** Telechargement controle (version courante) : le droit de lecture est verifie avant l'envoi des octets. */
    ProfileViews.DocumentFile download(Long documentId);

    /** Telechargement d'une version precise de l'historique (1 = depot initial), memes controles. */
    ProfileViews.DocumentFile downloadVersion(Long documentId, int version);
}
