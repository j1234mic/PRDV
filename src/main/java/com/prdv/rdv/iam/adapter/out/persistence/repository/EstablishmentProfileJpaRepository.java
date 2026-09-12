package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.EstablishmentProfileEntity;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstablishmentProfileJpaRepository extends JpaRepository<EstablishmentProfileEntity, Long> {

    Optional<EstablishmentProfileEntity> findByUserId(Long userId);

    @Query("select e from EstablishmentProfileEntity e where e.userId in "
            + "(select u.id from UserEntity u where u.status = :status)")
    List<EstablishmentProfileEntity> findPendingValidation(@Param("status") AccountStatus status);
}
