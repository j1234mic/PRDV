package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifie que Hibernate peut creer l'EntityManagerFactory et stocker les confiances Double. */
@DataJpaTest
class MedicalDocumentEntityJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void confidenceValuesCanBePersistedAndRead() {
        MedicalDocumentEntity document = new MedicalDocumentEntity();
        document.setOwnerUserId(42L);
        document.setStatus(MedicalDocument.DocumentStatus.CLASSIFIED);
        document.setClassificationConfidence(0.9375);
        document.setOcrConfidence(0.8125);

        Long id = entityManager.persistAndFlush(document).getId();
        entityManager.clear();
        MedicalDocumentEntity saved = entityManager.find(MedicalDocumentEntity.class, id);

        assertThat(saved.getClassificationConfidence()).isEqualTo(0.9375);
        assertThat(saved.getOcrConfidence()).isEqualTo(0.8125);
    }
}
