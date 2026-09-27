package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PractitionerRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PractitionerRatingJpaRepository extends JpaRepository<PractitionerRatingEntity, String> {

    List<PractitionerRatingEntity> findByPractitionerUserIdOrderByCreatedAtDesc(Long practitionerUserId);

    Optional<PractitionerRatingEntity> findByPractitionerUserIdAndPatientUserId(Long practitionerUserId,
                                                                                Long patientUserId);

    void deleteByPatientUserId(Long patientUserId);
}
