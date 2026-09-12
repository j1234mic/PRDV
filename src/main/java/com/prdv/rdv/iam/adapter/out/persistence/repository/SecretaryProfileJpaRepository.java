package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.SecretaryProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SecretaryProfileJpaRepository extends JpaRepository<SecretaryProfileEntity, Long> {

    Optional<SecretaryProfileEntity> findByUserId(Long userId);

    @Query("select s from SecretaryProfileEntity s join s.supervisedPractitionerIds p where p = :practitionerId")
    List<SecretaryProfileEntity> findBySupervisedPractitionerId(@Param("practitionerId") Long practitionerId);
}
