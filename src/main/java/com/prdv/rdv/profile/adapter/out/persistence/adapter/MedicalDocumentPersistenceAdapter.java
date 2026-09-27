package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.entity.DocumentShareEntity;
import com.prdv.rdv.profile.adapter.out.persistence.entity.MedicalDocumentEntity;
import com.prdv.rdv.profile.adapter.out.persistence.mapper.MedicalPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.DocumentShareJpaRepository;
import com.prdv.rdv.profile.adapter.out.persistence.repository.MedicalDocumentJpaRepository;
import com.prdv.rdv.profile.application.port.output.MedicalDocumentRepository;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapteur JPA des documents medicaux.
 *
 * <p>Les partages vivent dans leur propre table (controle d'acces interrogeable) ;
 * ils sont ecrasables par fusion (merge) et ne sont jamais supprimes : un
 * partage revoque conserve sa trace (revokedAt), ce qui garantit la
 * tracabilite des acces aux donnees de sante.
 */
@Repository
public class MedicalDocumentPersistenceAdapter implements MedicalDocumentRepository {

    private final MedicalDocumentJpaRepository documents;
    private final DocumentShareJpaRepository shares;
    private final MedicalPersistenceMapper mapper;

    public MedicalDocumentPersistenceAdapter(MedicalDocumentJpaRepository documents,
                                             DocumentShareJpaRepository shares,
                                             MedicalPersistenceMapper mapper) {
        this.documents = documents;
        this.shares = shares;
        this.mapper = mapper;
    }

    @Override
    public MedicalDocument save(MedicalDocument document) {
        MedicalDocumentEntity entity = mapper.toEntity(document);
        MedicalDocumentEntity saved = documents.save(entity);

        List<DocumentShareEntity> shareEntities = document.getShares().stream()
                .map(share -> mapper.toEntity(share, saved.getId(), saved.getOwnerUserId()))
                .toList();
        shares.saveAll(shareEntities);

        return mapper.toDomain(saved, shares.findByDocumentId(saved.getId()));
    }

    @Override
    public Optional<MedicalDocument> findById(Long id) {
        return documents.findById(id)
                .map(entity -> mapper.toDomain(entity, shares.findByDocumentId(id)));
    }

    @Override
    public List<MedicalDocument> findByOwnerUserId(Long ownerUserId) {
        return documents.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
                .map(entity -> mapper.toDomain(entity, shares.findByDocumentId(entity.getId())))
                .toList();
    }

    @Override
    public List<MedicalDocument> findSharedWith(Long granteeUserId) {
        Instant now = Instant.now();
        List<Long> documentIds = shares.findByGranteeUserIdAndRevokedAtIsNull(granteeUserId).stream()
                .filter(share -> share.getExpiresAt() == null || share.getExpiresAt().isAfter(now))
                .map(DocumentShareEntity::getDocumentId)
                .distinct()
                .toList();
        return documents.findAllById(documentIds).stream()
                .map(entity -> mapper.toDomain(entity, shares.findByDocumentId(entity.getId())))
                .toList();
    }

    @Override
    public void deleteByOwnerUserId(Long ownerUserId) {
        shares.deleteByOwnerUserId(ownerUserId);
        documents.deleteByOwnerUserId(ownerUserId);
    }
}
