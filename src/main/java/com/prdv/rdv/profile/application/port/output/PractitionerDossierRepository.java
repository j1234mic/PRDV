package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;

import java.util.List;
import java.util.Optional;

/** Persistance du dossier professionnel des praticiens. */
public interface PractitionerDossierRepository {

    PractitionerDossier save(PractitionerDossier dossier);

    Optional<PractitionerDossier> findByUserId(Long userId);

    /** Recherche d'annuaire : tous les filtres sont optionnels (null = ignore). */
    List<PractitionerDossier> search(String term, String specialty, boolean teleconsultationOnly,
                                     int offset, int limit);

    long countSearch(String term, String specialty, boolean teleconsultationOnly);

    void deleteByUserId(Long userId);
}
