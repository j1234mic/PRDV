package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.port.input.AuthenticationUseCase;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.FraudDetectionPort;
import com.prdv.rdv.iam.application.port.output.JwtTokenPort;
import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.application.port.output.TotpPort;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.OtpIssuer;
import com.prdv.rdv.iam.application.service.support.TokenIssuanceSupport;
import com.prdv.rdv.iam.application.service.support.TransactionalStateSaver;
import com.prdv.rdv.iam.config.IamProperties;
import com.prdv.rdv.iam.domain.event.DomainEvent;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import com.prdv.rdv.iam.domain.model.user.Email;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Service applicatif central d'authentification.
 * Ordonne : controle du mot de passe, politique de verrouillage, MFA TOTP,
 * OTP de secours, detection de fraude, emission des jetons et audit.
 */
@Service
public class AuthenticationService implements AuthenticationUseCase {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenPort jwtTokenPort;
    private final TotpPort totpPort;
    private final FraudDetectionPort fraudDetection;
    private final OtpIssuer otpIssuer;
    private final TokenIssuanceSupport tokenIssuance;
    private final TransactionalStateSaver stateSaver;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final IamProperties properties;
    private final Clock clock;

    public AuthenticationService(UserRepository userRepository, UserSessionRepository sessionRepository,
                                 RefreshTokenRepository refreshTokenRepository, PasswordHasher passwordHasher,
                                 JwtTokenPort jwtTokenPort, TotpPort totpPort, FraudDetectionPort fraudDetection,
                                 OtpIssuer otpIssuer, TokenIssuanceSupport tokenIssuance,
                                 TransactionalStateSaver stateSaver,
                                 DomainEventPublisher eventPublisher, AuditLogger auditLogger,
                                 IamProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.jwtTokenPort = jwtTokenPort;
        this.totpPort = totpPort;
        this.fraudDetection = fraudDetection;
        this.otpIssuer = otpIssuer;
        this.tokenIssuance = tokenIssuance;
        this.stateSaver = stateSaver;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthResults.AuthResult login(AuthCommands.Login cmd) {
        RequestMetadata metadata = cmd.metadata() == null ? RequestMetadata.unknown() : cmd.metadata();
        String email;
        try {
            email = Email.of(cmd.email()).value();
        } catch (IamException e) {
            throw IamException.of(IamErrorCode.INVALID_CREDENTIALS, "Email ou mot de passe incorrect");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !passwordHasher.matches(cmd.password(), user.getPasswordHash())) {
            if (user != null) {
                registerFailedAttempt(user, metadata.ipAddress());
            }
            throw IamException.of(IamErrorCode.INVALID_CREDENTIALS, "Email ou mot de passe incorrect");
        }

        // Verrouillage / suspension / email non confirme
        user.assertCanAuthenticate(clock);

        // Facteur 2 : TOTP (application d'authentification)
        if (user.isMfaEnabled()) {
            String challenge = jwtTokenPort.createChallengeToken(user.getId(), "MFA");
            return AuthResults.AuthResult.challengeRequired(
                    new AuthResults.Challenge(challenge, "MFA_TOTP", user.getEmail()));
        }

        // Detection de fraude : nouvel appareil / nouveau pays / impossible travel
        UserSession previous = sessionRepository.findLatestActiveByUserId(user.getId()).orElse(null);
        FraudDetectionPort.FraudAssessment assessment = fraudDetection.assess(
                new FraudDetectionPort.FraudContext(user.getId(), metadata.ipAddress(),
                        metadata.geoLocation(), previous));

        if (assessment.stepUpRequired()) {
            otpIssuer.issue(user.getEmail(), OtpChallenge.Channel.EMAIL, OtpChallenge.Purpose.LOGIN);
            String reason = String.join(", ", assessment.reasons());
            eventPublisher.publish(new DomainEvent.SuspiciousLoginDetected(
                    user.getId(), reason, metadata.ipAddress(), clock.instant()));
            auditLogger.record(user.getId(), AuditLog.Action.LOGIN_SUSPICIOUS, AuditLog.Outcome.SUCCESS,
                    "Session", null, reason, metadata.ipAddress());
            String challenge = jwtTokenPort.createChallengeToken(user.getId(), "OTP");
            return AuthResults.AuthResult.challengeRequired(
                    new AuthResults.Challenge(challenge, "OTP_EMAIL", user.getEmail()));
        }

        return AuthResults.AuthResult.authenticated(tokenIssuance.completeLogin(user, metadata, false));
    }

    @Override
    @Transactional
    public AuthResults.AuthResult verifyTotp(AuthCommands.VerifyTotp cmd) {
        JwtTokenPort.ChallengeClaims claims = jwtTokenPort.parseChallengeToken(cmd.challengeToken(), "MFA");
        User user = requireUser(claims.userId());
        user.assertCanAuthenticate(clock);

        if (!user.isMfaEnabled() || user.getTotpSecret() == null
                || !totpPort.verifyCode(user.getTotpSecret(), cmd.code())) {
            auditLogger.failure(user.getId(), AuditLog.Action.LOGIN_FAILED, "code TOTP invalide",
                    cmd.metadata() == null ? null : cmd.metadata().ipAddress());
            throw IamException.of(IamErrorCode.MFA_INVALID_CODE, "Code d'authentification invalide");
        }
        RequestMetadata metadata = cmd.metadata() == null ? RequestMetadata.unknown() : cmd.metadata();
        return AuthResults.AuthResult.authenticated(tokenIssuance.completeLogin(user, metadata, false));
    }

    @Override
    @Transactional
    public AuthResults.AuthResult verifyLoginOtp(AuthCommands.VerifyLoginOtp cmd) {
        JwtTokenPort.ChallengeClaims claims = jwtTokenPort.parseChallengeToken(cmd.challengeToken(), "OTP");
        User user = requireUser(claims.userId());
        otpIssuer.verifyLatest(user.getEmail(), OtpChallenge.Purpose.LOGIN, cmd.code());
        RequestMetadata metadata = cmd.metadata() == null ? RequestMetadata.unknown() : cmd.metadata();
        return AuthResults.AuthResult.authenticated(tokenIssuance.completeLogin(user, metadata, true));
    }

    @Override
    @Transactional
    public AuthResults.TokenSet refresh(AuthCommands.Refresh cmd) {
        RequestMetadata metadata = cmd.metadata() == null ? RequestMetadata.unknown() : cmd.metadata();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(passwordHasher.digest(cmd.refreshToken()))
                .orElseThrow(() -> IamException.of(IamErrorCode.TOKEN_INVALID, "Jeton de rafraichissement inconnu"));

        // Detection de reutilisation : revoquer toute la famille (theorie du jeton vole).
        // Sauvegarde en REQUIRES_NEW via stateSaver : les revocations doivent
        // persister malgre l'exception fonctionnelle qui provoque le rollback
        // de la transaction appelante.
        if (stored.isRevoked()) {
            refreshTokenRepository.findAllByFamily(stored.getFamily()).forEach(t -> {
                if (!t.isRevoked()) {
                    t.revoke(clock);
                    stateSaver.saveRefreshToken(t);
                }
            });
            auditLogger.failure(stored.getUserId(), AuditLog.Action.LOGIN_FAILED,
                    "reutilisation d'un refresh token revoque", metadata.ipAddress());
            throw IamException.of(IamErrorCode.REFRESH_TOKEN_REUSED,
                    "Reutilisation d'un jeton revoque : toutes les sessions ont ete coupees");
        }
        if (!stored.isValid(clock)) {
            throw IamException.of(IamErrorCode.TOKEN_EXPIRED, "Session expiree, reconnectez-vous");
        }

        User user = requireUser(stored.getUserId());
        user.assertCanAuthenticate(clock);
        return tokenIssuance.rotate(user, stored, metadata);
    }

    @Override
    @Transactional
    public void logout(AuthCommands.Logout cmd) {
        refreshTokenRepository.findByTokenHash(passwordHasher.digest(cmd.refreshToken())).ifPresent(token -> {
            token.revoke(clock);
            refreshTokenRepository.save(token);
            sessionRepository.findById(token.getSessionId()).ifPresent(session -> {
                session.revoke(clock);
                sessionRepository.save(session);
            });
            auditLogger.success(token.getUserId(), AuditLog.Action.LOGOUT, "deconnexion");
        });
    }

    private void registerFailedAttempt(User user, String ip) {
        int max = properties.getSecurity().getLogin().getMaxFailedAttempts();
        Duration lock = Duration.ofMinutes(properties.getSecurity().getLogin().getLockDurationMinutes());
        user.recordFailedLogin(max, lock, clock);
        // Sauvegarde dans une transaction independante : l'etat (failedAttempts,
        // LOCKED) doit persister malgre l'exception INVALID_CREDENTIALS qui
        // provoque le rollback de la transaction appelante.
        User saved = stateSaver.saveUser(user);
        AuditLog.Action action = saved.getStatus() == com.prdv.rdv.iam.domain.model.user.AccountStatus.LOCKED
                ? AuditLog.Action.ACCOUNT_LOCKED : AuditLog.Action.LOGIN_FAILED;
        auditLogger.failure(user.getId(), action, "echec de connexion (mot de passe)", ip);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> IamException.of(IamErrorCode.TOKEN_INVALID, "Compte introuvable"));
    }
}
