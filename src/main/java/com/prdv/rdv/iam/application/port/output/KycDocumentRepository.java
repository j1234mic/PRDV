package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.verification.KycDocument;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository {

    KycDocument save(KycDocument document);

    Optional<KycDocument> findById(Long id);

    List<KycDocument> findByOwnerUserId(Long ownerUserId);

    List<KycDocument> findPending(int page, int size);
}
