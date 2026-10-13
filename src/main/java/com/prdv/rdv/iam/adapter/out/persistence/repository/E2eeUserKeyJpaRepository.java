package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.E2eeUserKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface E2eeUserKeyJpaRepository extends JpaRepository<E2eeUserKeyEntity, Long> {

    Optional<E2eeUserKeyEntity> findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    List<E2eeUserKeyEntity> findByUserId(Long userId);
}
