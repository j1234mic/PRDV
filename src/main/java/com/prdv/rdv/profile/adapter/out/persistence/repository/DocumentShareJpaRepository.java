package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.DocumentShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DocumentShareJpaRepository extends JpaRepository<DocumentShareEntity, String> {

    List<DocumentShareEntity> findByDocumentId(Long documentId);

    List<DocumentShareEntity> findByGranteeUserId(Long granteeUserId);

    /** Partages actifs d'un destinataire (non revoques et non expires). */
    List<DocumentShareEntity> findByGranteeUserIdAndRevokedAtIsNull(Long granteeUserId);

    void deleteByDocumentId(Long documentId);

    void deleteByOwnerUserId(Long ownerUserId);

    /** Utilise pour purger les partages expires (tache de maintenance). */
    List<DocumentShareEntity> findByExpiresAtBefore(Instant instant);
}
