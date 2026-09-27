package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.MedicalDocumentEntity;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalDocumentJpaRepository extends JpaRepository<MedicalDocumentEntity, Long> {

    List<MedicalDocumentEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<MedicalDocumentEntity> findByOwnerUserIdAndCategoryOrderByCreatedAtDesc(
            Long ownerUserId, MedicalDocument.DocumentCategory category);

    void deleteByOwnerUserId(Long ownerUserId);

    /** Garde-fou : permet de borner un parcours complet si besoin (page par page). */
    List<MedicalDocumentEntity> findAllBy(Pageable pageable);
}
