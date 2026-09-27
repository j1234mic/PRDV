package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.mapper.PractitionerPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.PracticeLocationJpaRepository;
import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adapteur JPA des lieux d'exercice. */
@Repository
public class PracticeLocationPersistenceAdapter implements PracticeLocationRepository {

    private final PracticeLocationJpaRepository jpa;
    private final PractitionerPersistenceMapper mapper;

    public PracticeLocationPersistenceAdapter(PracticeLocationJpaRepository jpa,
                                              PractitionerPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PracticeLocation save(PracticeLocation location) {
        return mapper.toDomain(jpa.save(mapper.toEntity(location)));
    }

    @Override
    public Optional<PracticeLocation> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PracticeLocation> findByPractitionerUserId(Long practitionerUserId) {
        return jpa.findByPractitionerUserIdOrderByMainLocationDesc(practitionerUserId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PracticeLocation> findByCity(String city) {
        return jpa.findByCityIgnoreCase(city).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpa.deleteById(id);
    }

    @Override
    public void deleteByPractitionerUserId(Long practitionerUserId) {
        jpa.deleteByPractitionerUserId(practitionerUserId);
    }
}
