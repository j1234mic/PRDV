package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.mapper.PractitionerPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.PractitionerDossierJpaRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerDossierRepository;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adapteur JPA du dossier professionnel des praticiens (avec recherche d'annuaire). */
@Repository
public class PractitionerDossierPersistenceAdapter implements PractitionerDossierRepository {

    private final PractitionerDossierJpaRepository jpa;
    private final PractitionerPersistenceMapper mapper;

    public PractitionerDossierPersistenceAdapter(PractitionerDossierJpaRepository jpa,
                                                 PractitionerPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PractitionerDossier save(PractitionerDossier dossier) {
        var existing = jpa.findByUserId(dossier.getUserId()).orElse(null);
        var entity = mapper.toEntity(dossier);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<PractitionerDossier> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public List<PractitionerDossier> search(String term, String specialty, boolean teleconsultationOnly,
                                            int offset, int limit) {
        int page = limit <= 0 ? 0 : offset / limit;
        return jpa.search(normalize(term), normalize(specialty), teleconsultationOnly,
                        PageRequest.of(Math.max(page, 0), Math.max(limit, 1))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countSearch(String term, String specialty, boolean teleconsultationOnly) {
        return jpa.countSearch(normalize(term), normalize(specialty), teleconsultationOnly);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
