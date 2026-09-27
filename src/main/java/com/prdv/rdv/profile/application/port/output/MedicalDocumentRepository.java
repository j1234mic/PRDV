package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.document.MedicalDocument;

import java.util.List;
import java.util.Optional;

/** Persistance des documents medicaux (metadonnees, versions, partages). */
public interface MedicalDocumentRepository {

    MedicalDocument save(MedicalDocument document);

    Optional<MedicalDocument> findById(Long id);

    List<MedicalDocument> findByOwnerUserId(Long ownerUserId);

    /** Documents partages avec un utilisateur et toujours actifs a la date donnee. */
    List<MedicalDocument> findSharedWith(Long granteeUserId);

    void deleteByOwnerUserId(Long ownerUserId);
}
