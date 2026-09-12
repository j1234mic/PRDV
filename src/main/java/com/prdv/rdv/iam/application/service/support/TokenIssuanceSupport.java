package com.prdv.rdv.iam.application.service.support;

import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.port.input.AuthorizationQueryUseCase;
import com.prdv.rdv.iam.application.port.output.JwtTokenPort;
import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.RandomCodeGenerator;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.config.IamProperties;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Emission des jetons et sessions : access token JWT, refresh token opaque
 * tournant (rotation), journalisation des connexions.
 */
@Component
public class TokenIssuanceSupport {

    private final JwtTokenPort jwtTokenPort;
    private final RandomCodeGenerator randomCodeGenerator;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuthorizationQueryUseCase authorizationQuery;
    private final IamProperties properties;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public TokenIssuanceSupport(JwtTokenPort jwtTokenPort, RandomCodeGenerator randomCodeGenerator,
                                PasswordHasher passwordHasher, RefreshTokenRepository refreshTokenRepository,
                                UserSessionRepository sessionRepository, UserRepository userRepository,
                                AuthorizationQueryUseCase authorizationQuery, IamProperties properties,
                                AuditLogger auditLogger, Clock clock) {
        this.jwtTokenPort = jwtTokenPort;
        this.randomCodeGenerator = randomCodeGenerator;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.authorizationQuery = authorizationQuery;
        this.properties = properties;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    /** Finalise une connexion reussie : reset des echecs, session + jetons. */
    @Transactional
    public AuthResults.TokenSet completeLogin(User user, RequestMetadata metadata, boolean suspicious) {
        user.resetFailedLogins(clock);
        userRepository.save(user);
        UserSession session = UserSession.start(user.getId(), metadata.ipAddress(),
                metadata.userAgent(), deviceLabel(metadata.userAgent()),
                metadata.geoLocation(), suspicious, clock);
        session = sessionRepository.save(session);

        AuthResults.TokenSet tokens = issueNewRefreshToken(user, session, randomCodeGenerator.opaqueToken());
        auditLogger.record(user.getId(), AuditLog.Action.LOGIN_SUCCESS, AuditLog.Outcome.SUCCESS,
                "Session", String.valueOf(session.getId()),
                suspicious ? "connexion suspecte - step up reussi" : "connexion",
                metadata.ipAddress());
        return tokens;
    }

    /** Rotation : l'ancien jeton est revoque, un nouveau est emis dans la meme famille. */
    @Transactional
    public AuthResults.TokenSet rotate(User user, RefreshToken previous, RequestMetadata metadata) {
        previous.revoke(clock);
        refreshTokenRepository.save(previous);

        UserSession session = sessionRepository.findById(previous.getSessionId())
                .orElseGet(() -> UserSession.start(user.getId(), metadata.ipAddress(),
                        metadata.userAgent(), deviceLabel(metadata.userAgent()),
                        metadata.geoLocation(), false, clock));
        session.touch(clock);
        session = sessionRepository.save(session);

        AuthResults.TokenSet tokens = issueNewRefreshToken(user, session, previous.getFamily());
        auditLogger.success(user.getId(), AuditLog.Action.TOKEN_REFRESHED, "rotation refresh token");
        return tokens;
    }

    private AuthResults.TokenSet issueNewRefreshToken(User user, UserSession session, String family) {
        String rawToken = randomCodeGenerator.opaqueToken();
        RefreshToken refreshToken = RefreshToken.create(user.getId(),
                passwordHasher.digest(rawToken), family, session.getId(),
                properties.getSecurity().getRefresh().getTokenTtlDays(), clock);
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtTokenPort.createAccessToken(user.getId(), user.getEmail(),
                authorizationQuery.authoritiesFor(user.getId()), user.getTokenVersion());
        return AuthResults.TokenSet.bearer(accessToken, rawToken,
                jwtTokenPort.getAccessTokenTtlSeconds());
    }

    private static String deviceLabel(String userAgent) {
        if (userAgent == null) {
            return "Appareil inconnu";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        }
        if (ua.contains("windows")) {
            return "Ordinateur Windows";
        }
        if (ua.contains("mac os")) {
            return "Ordinateur macOS";
        }
        if (ua.contains("linux")) {
            return "Ordinateur Linux";
        }
        return "Navigateur web";
    }
}
