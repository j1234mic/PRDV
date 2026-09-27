package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;

import java.util.Optional;

/** Persistance de l'identite patient (un profil par utilisateur). */
public interface PatientIdentityRepository {

    PatientIdentity save(PatientIdentity identity);

    Optional<PatientIdentity> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
