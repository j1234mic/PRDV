package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PractitionerDossierEntity;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PractitionerDossierJpaRepository extends JpaRepository<PractitionerDossierEntity, Long> {

    Optional<PractitionerDossierEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    @Query("""
            select d from PractitionerDossierEntity d
            where (:specialty is null or lower(d.mainSpecialty) = lower(:specialty))
              and (:teleconsultationOnly = false or d.teleconsultation = true)
              and (:term is null
                   or lower(d.firstName) like lower(concat('%', :term, '%'))
                   or lower(d.lastName) like lower(concat('%', :term, '%'))
                   or lower(d.mainSpecialty) like lower(concat('%', :term, '%')))
            order by d.lastName asc, d.firstName asc
            """)
    List<PractitionerDossierEntity> search(@Param("term") String term,
                                           @Param("specialty") String specialty,
                                           @Param("teleconsultationOnly") boolean teleconsultationOnly,
                                           Pageable pageable);

    @Query("""
            select count(d) from PractitionerDossierEntity d
            where (:specialty is null or lower(d.mainSpecialty) = lower(:specialty))
              and (:teleconsultationOnly = false or d.teleconsultation = true)
              and (:term is null
                   or lower(d.firstName) like lower(concat('%', :term, '%'))
                   or lower(d.lastName) like lower(concat('%', :term, '%'))
                   or lower(d.mainSpecialty) like lower(concat('%', :term, '%')))
            """)
    long countSearch(@Param("term") String term,
                     @Param("specialty") String specialty,
                     @Param("teleconsultationOnly") boolean teleconsultationOnly);

    /** Dossiers publies par secteur de convention (utile aux etudes de couverture). */
    List<PractitionerDossierEntity> findBySector(PractitionerDossier.ConventionSector sector);
}
