package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.KycDocumentEntity;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentJpaRepository extends JpaRepository<KycDocumentEntity, Long> {

    List<KycDocumentEntity> findByOwnerUserIdOrderByUploadedAtDesc(Long ownerUserId);

    List<KycDocumentEntity> findByStatusOrderByUploadedAtAsc(KycDocument.ReviewStatus status, Pageable pageable);
}
