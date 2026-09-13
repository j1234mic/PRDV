package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.OtpChallengeEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.OtpChallengeJpaRepository;
import com.prdv.rdv.iam.application.port.output.OtpChallengeRepository;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapteur JPA des challenges OTP (codes a usage unique).
 *
 * <p>Un adapteur par port de sortie : les ports sont fins (segregation
 * d'interfaces), leurs implementations le restent aussi.
 */
@Repository
public class OtpChallengePersistenceAdapter implements OtpChallengeRepository {

    private final OtpChallengeJpaRepository otpJpa;
    private final AuthPersistenceMapper mapper;

    public OtpChallengePersistenceAdapter(OtpChallengeJpaRepository otpJpa,
                                          AuthPersistenceMapper mapper) {
        this.otpJpa = otpJpa;
        this.mapper = mapper;
    }

    @Override
    public OtpChallenge save(OtpChallenge challenge) {
        OtpChallengeEntity entity = challenge.getId() == null
                ? new OtpChallengeEntity()
                : otpJpa.findById(challenge.getId()).orElseGet(OtpChallengeEntity::new);
        OtpChallengeEntity mapped = mapper.toEntity(challenge);
        mapped.setId(entity.getId());
        return mapper.toDomain(otpJpa.save(mapped));
    }

    @Override
    public Optional<OtpChallenge> findLatestActive(String target, OtpChallenge.Purpose purpose) {
        return otpJpa
                .findTopByTargetAndPurposeAndConsumedFalseOrderByCreatedAtDesc(target, purpose)
                .map(mapper::toDomain);
    }
}
