package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.practitioner.PractitionerRating;

import java.util.List;
import java.util.Optional;

/** Persistance des avis patients. */
public interface PractitionerRatingRepository {

    PractitionerRating save(PractitionerRating rating);

    List<PractitionerRating> findByPractitionerUserId(Long practitionerUserId);

    Optional<PractitionerRating> findByPractitionerAndPatient(Long practitionerUserId, Long patientUserId);

    void deleteByPatientUserId(Long patientUserId);
}
