package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionJpaRepository extends JpaRepository<UserSessionEntity, Long> {

    List<UserSessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserSessionEntity> findTopByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(Long userId);
}
