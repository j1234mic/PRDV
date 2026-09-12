package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.AuditLogEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.OtpChallengeEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.RefreshTokenEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.SocialAccountEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.UserSessionEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.AuditLogJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.OtpChallengeJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.SocialAccountJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.UserSessionJpaRepository;
import com.prdv.rdv.iam.application.port.output.AuditLogRepository;
import com.prdv.rdv.iam.application.port.output.OtpChallengeRepository;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.application.port.output.SocialAccountRepository;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import com.prdv.rdv.iam.domain.model.auth.SocialAccount;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapteur JPA des agregats d'authentification : OTP, refresh tokens,
 * sessions, comptes sociaux et journal d'audit.
 */
@Repository
public class AuthPersistenceAdapter
        implements OtpChallengeRepository, RefreshTokenRepository,
        UserSessionRepository, SocialAccountRepository, AuditLogRepository {

    private final OtpChallengeJpaRepository otpJpa;
    private final RefreshTokenJpaRepository refreshJpa;
    private final UserSessionJpaRepository sessionJpa;
    private final SocialAccountJpaRepository socialJpa;
    private final AuditLogJpaRepository auditJpa;
    private final AuthPersistenceMapper mapper;

    public AuthPersistenceAdapter(OtpChallengeJpaRepository otpJpa, RefreshTokenJpaRepository refreshJpa,
                                  UserSessionJpaRepository sessionJpa, SocialAccountJpaRepository socialJpa,
                                  AuditLogJpaRepository auditJpa, AuthPersistenceMapper mapper) {
        this.otpJpa = otpJpa;
        this.refreshJpa = refreshJpa;
        this.sessionJpa = sessionJpa;
        this.socialJpa = socialJpa;
        this.auditJpa = auditJpa;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- OTP
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

    // ------------------------------------------------------- Refresh token
    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = token.getId() == null
                ? new RefreshTokenEntity()
                : refreshJpa.findById(token.getId()).orElseGet(RefreshTokenEntity::new);
        RefreshTokenEntity mapped = mapper.toEntity(token);
        mapped.setId(entity.getId());
        return mapper.toDomain(refreshJpa.save(mapped));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshJpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<RefreshToken> findAllByFamily(String family) {
        return refreshJpa.findAllByFamily(family).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<RefreshToken> findByUserId(Long userId) {
        return refreshJpa.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    // ------------------------------------------------------------- Session
    @Override
    public UserSession save(UserSession session) {
        UserSessionEntity entity = session.getId() == null
                ? new UserSessionEntity()
                : sessionJpa.findById(session.getId()).orElseGet(UserSessionEntity::new);
        UserSessionEntity mapped = mapper.toEntity(session);
        mapped.setId(entity.getId());
        return mapper.toDomain(sessionJpa.save(mapped));
    }

    @Override
    public Optional<UserSession> findById(Long id) {
        return sessionJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<UserSession> findByUserId(Long userId) {
        return sessionJpa.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<UserSession> findLatestActiveByUserId(Long userId) {
        return sessionJpa.findTopByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId)
                .map(mapper::toDomain);
    }

    // -------------------------------------------------------------- Social
    @Override
    public SocialAccount save(SocialAccount account) {
        return mapper.toDomain(socialJpa.save(mapper.toEntity(account)));
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderUserId(SocialAccount.Provider provider,
                                                                    String providerUserId) {
        return socialJpa.findByProviderAndProviderUserId(provider, providerUserId).map(mapper::toDomain);
    }

    @Override
    public Optional<SocialAccount> findByUserIdAndProvider(Long userId, SocialAccount.Provider provider) {
        return socialJpa.findByUserIdAndProvider(userId, provider).map(mapper::toDomain);
    }

    // ---------------------------------------------------------------- Audit
    @Override
    public AuditLog save(AuditLog logEntry) {
        return mapper.toDomain(auditJpa.save(mapper.toEntity(logEntry)));
    }

    @Override
    public List<AuditLog> findAll(int page, int size) {
        return auditJpa.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByUserId(Long userId, int page, int size) {
        return auditJpa.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return auditJpa.count();
    }
}
