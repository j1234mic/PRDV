package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;

import java.util.List;
import java.util.Optional;

/** Persistance des lieux d'exercice. */
public interface PracticeLocationRepository {

    PracticeLocation save(PracticeLocation location);

    Optional<PracticeLocation> findById(String id);

    List<PracticeLocation> findByPractitionerUserId(Long practitionerUserId);

    /** Lieux d'exercice situes dans une ville donnee (recherche d'annuaire). */
    List<PracticeLocation> findByCity(String city);

    void deleteById(String id);

    void deleteByPractitionerUserId(Long practitionerUserId);
}
