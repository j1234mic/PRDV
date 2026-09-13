package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.KycDocumentEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.VerificationPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.KycDocumentJpaRepository;
import com.prdv.rdv.iam.application.port.output.KycDocumentRepository;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapteur JPA des pieces justificatives KYC (CNI, passeport, diplome, RIB,
 * attestation RC pro, contrat signe) : seule la cle de stockage est persistee.
 */
@Repository
public class KycDocumentPersistenceAdapter implements KycDocumentRepository {

    private final KycDocumentJpaRepository kycJpa;
    private final VerificationPersistenceMapper mapper;

    public KycDocumentPersistenceAdapter(KycDocumentJpaRepository kycJpa,
                                         VerificationPersistenceMapper mapper) {
        this.kycJpa = kycJpa;
        this.mapper = mapper;
    }

    @Override
    public KycDocument save(KycDocument document) {
        KycDocumentEntity existing = document.getId() == null ? null
                : kycJpa.findById(document.getId()).orElse(null);
        KycDocumentEntity mapped = mapper.toEntity(document);
        if (existing != null) {
            mapped.setId(existing.getId());
            mapped.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(kycJpa.save(mapped));
    }

    @Override
    public Optional<KycDocument> findById(Long id) {
        return kycJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<KycDocument> findByOwnerUserId(Long ownerUserId) {
        return kycJpa.findByOwnerUserIdOrderByUploadedAtDesc(ownerUserId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<KycDocument> findPending(int page, int size) {
        return kycJpa.findByStatusOrderByUploadedAtAsc(KycDocument.ReviewStatus.PENDING,
                        PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }
}
