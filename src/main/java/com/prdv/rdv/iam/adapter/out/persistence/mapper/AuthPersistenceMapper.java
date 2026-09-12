package com.prdv.rdv.iam.adapter.out.persistence.mapper;

import com.prdv.rdv.iam.adapter.out.persistence.entity.AuditLogEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.DelegationEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.GeoLocationEmbeddable;
import com.prdv.rdv.iam.adapter.out.persistence.entity.OtpChallengeEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.RefreshTokenEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.SocialAccountEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.UserSessionEntity;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.Delegation;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import com.prdv.rdv.iam.domain.model.auth.SocialAccount;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class AuthPersistenceMapper {

    // --------------------------------------------------------------- OTP
    public OtpChallengeEntity toEntity(OtpChallenge d) {
        OtpChallengeEntity e = new OtpChallengeEntity();
        e.setId(d.getId());
        e.setTarget(d.getTarget());
        e.setChannel(d.getChannel());
        e.setPurpose(d.getPurpose());
        e.setCodeHash(d.getCodeHash());
        e.setExpiresAt(d.getExpiresAt());
        e.setAttempts(d.getAttempts());
        e.setConsumed(d.isConsumed());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    public OtpChallenge toDomain(OtpChallengeEntity e) {
        OtpChallenge d = new OtpChallenge();
        d.setId(e.getId());
        d.setTarget(e.getTarget());
        d.setChannel(e.getChannel());
        d.setPurpose(e.getPurpose());
        d.setCodeHash(e.getCodeHash());
        d.setExpiresAt(e.getExpiresAt());
        d.setAttempts(e.getAttempts());
        d.setConsumed(e.isConsumed());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }

    // ------------------------------------------------------- Refresh token
    public RefreshTokenEntity toEntity(RefreshToken d) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setTokenHash(d.getTokenHash());
        e.setFamily(d.getFamily());
        e.setSessionId(d.getSessionId());
        e.setExpiresAt(d.getExpiresAt());
        e.setRevoked(d.isRevoked());
        e.setRevokedAt(d.getRevokedAt());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    public RefreshToken toDomain(RefreshTokenEntity e) {
        RefreshToken d = new RefreshToken();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setTokenHash(e.getTokenHash());
        d.setFamily(e.getFamily());
        d.setSessionId(e.getSessionId());
        d.setExpiresAt(e.getExpiresAt());
        d.setRevoked(e.isRevoked());
        d.setRevokedAt(e.getRevokedAt());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }

    // ------------------------------------------------------------- Session
    public UserSessionEntity toEntity(UserSession d) {
        UserSessionEntity e = new UserSessionEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setIpAddress(d.getIpAddress());
        e.setUserAgent(d.getUserAgent());
        e.setDeviceLabel(d.getDeviceLabel());
        UserSession.GeoLocation g = d.getGeoLocation();
        if (g != null) {
            e.setGeoLocation(new GeoLocationEmbeddable(g.country(), g.city(), g.latitude(), g.longitude()));
        }
        e.setSuspicious(d.isSuspicious());
        e.setLastSeenAt(d.getLastSeenAt());
        e.setRevokedAt(d.getRevokedAt());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    public UserSession toDomain(UserSessionEntity e) {
        UserSession d = new UserSession();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setIpAddress(e.getIpAddress());
        d.setUserAgent(e.getUserAgent());
        d.setDeviceLabel(e.getDeviceLabel());
        GeoLocationEmbeddable g = e.getGeoLocation();
        if (g != null) {
            d.setGeoLocation(new UserSession.GeoLocation(g.getCountry(), g.getCity(),
                    g.getLatitude(), g.getLongitude()));
        }
        d.setSuspicious(e.isSuspicious());
        d.setLastSeenAt(e.getLastSeenAt());
        d.setRevokedAt(e.getRevokedAt());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }

    // ---------------------------------------------------------- Delegation
    public DelegationEntity toEntity(Delegation d) {
        DelegationEntity e = new DelegationEntity();
        e.setId(d.getId());
        e.setGranterUserId(d.getGranterUserId());
        e.setGranteeUserId(d.getGranteeUserId());
        e.setPermissionCodes(new HashSet<>(d.getPermissionCodes()));
        e.setReason(d.getReason());
        e.setValidFrom(d.getValidFrom());
        e.setValidUntil(d.getValidUntil());
        e.setRevokedAt(d.getRevokedAt());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    public Delegation toDomain(DelegationEntity e) {
        Delegation d = new Delegation();
        d.setId(e.getId());
        d.setGranterUserId(e.getGranterUserId());
        d.setGranteeUserId(e.getGranteeUserId());
        d.setPermissionCodes(new HashSet<>(e.getPermissionCodes()));
        d.setReason(e.getReason());
        d.setValidFrom(e.getValidFrom());
        d.setValidUntil(e.getValidUntil());
        d.setRevokedAt(e.getRevokedAt());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }

    // --------------------------------------------------------------- Social
    public SocialAccountEntity toEntity(SocialAccount d) {
        SocialAccountEntity e = new SocialAccountEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setProvider(d.getProvider());
        e.setProviderUserId(d.getProviderUserId());
        e.setEmail(d.getEmail());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    public SocialAccount toDomain(SocialAccountEntity e) {
        SocialAccount d = new SocialAccount();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setProvider(e.getProvider());
        d.setProviderUserId(e.getProviderUserId());
        d.setEmail(e.getEmail());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }

    // ---------------------------------------------------------------- Audit
    public AuditLogEntity toEntity(AuditLog d) {
        AuditLogEntity e = new AuditLogEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setAction(d.getAction());
        e.setOutcome(d.getOutcome());
        e.setResourceType(d.getResourceType());
        e.setResourceId(d.getResourceId());
        e.setDetail(d.getDetail());
        e.setIpAddress(d.getIpAddress());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    public AuditLog toDomain(AuditLogEntity e) {
        AuditLog d = new AuditLog();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setAction(e.getAction());
        d.setOutcome(e.getOutcome());
        d.setResourceType(e.getResourceType());
        d.setResourceId(e.getResourceId());
        d.setDetail(e.getDetail());
        d.setIpAddress(e.getIpAddress());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }
}
