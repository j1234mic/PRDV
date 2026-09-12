package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.OtpChallengeEntity;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpChallengeJpaRepository extends JpaRepository<OtpChallengeEntity, Long> {

    Optional<OtpChallengeEntity> findTopByTargetAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            String target, OtpChallenge.Purpose purpose);
}
