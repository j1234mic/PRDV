package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.mapper.PractitionerPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.PractitionerRatingJpaRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerRatingRepository;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerRating;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adapteur JPA des avis patients. */
@Repository
public class PractitionerRatingPersistenceAdapter implements PractitionerRatingRepository {

    private final PractitionerRatingJpaRepository jpa;
    private final PractitionerPersistenceMapper mapper;

    public PractitionerRatingPersistenceAdapter(PractitionerRatingJpaRepository jpa,
                                                PractitionerPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PractitionerRating save(PractitionerRating rating) {
        return mapper.toDomain(jpa.save(mapper.toEntity(rating)));
    }

    @Override
    public List<PractitionerRating> findByPractitionerUserId(Long practitionerUserId) {
        return jpa.findByPractitionerUserIdOrderByCreatedAtDesc(practitionerUserId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PractitionerRating> findByPractitionerAndPatient(Long practitionerUserId,
                                                                     Long patientUserId) {
        return jpa.findByPractitionerUserIdAndPatientUserId(practitionerUserId, patientUserId)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByPatientUserId(Long patientUserId) {
        jpa.deleteByPatientUserId(patientUserId);
    }
}
