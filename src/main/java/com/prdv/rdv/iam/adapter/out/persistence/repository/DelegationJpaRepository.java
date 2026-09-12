package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.DelegationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DelegationJpaRepository extends JpaRepository<DelegationEntity, Long> {

    List<DelegationEntity> findByGranterUserId(Long granterUserId);

    @Query("select d from DelegationEntity d where d.granteeUserId = :userId and d.revokedAt is null "
            + "and (d.validFrom is null or d.validFrom <= :now) "
            + "and (d.validUntil is null or d.validUntil > :now)")
    List<DelegationEntity> findActiveByGrantee(@Param("userId") Long userId, @Param("now") Instant now);
}
