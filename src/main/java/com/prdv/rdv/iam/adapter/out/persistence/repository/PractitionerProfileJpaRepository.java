package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.PractitionerProfileEntity;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PractitionerProfileJpaRepository extends JpaRepository<PractitionerProfileEntity, Long> {

    Optional<PractitionerProfileEntity> findByUserId(Long userId);

    @Query("select p from PractitionerProfileEntity p where p.userId in "
            + "(select u.id from UserEntity u where u.status = :status)")
    List<PractitionerProfileEntity> findPendingValidation(@Param("status") AccountStatus status);
}
