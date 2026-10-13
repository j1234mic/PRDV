package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.port.input.BiometricAuthenticationUseCase;
import com.prdv.rdv.iam.application.port.output.BiometricCredentialRepository;
import com.prdv.rdv.iam.application.port.output.BiometricVerifierPort;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.JwtTokenPort;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.TokenIssuanceSupport;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.event.DomainEvent;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.BiometricCredential;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Service applicatif d'authentification biometrique (empreinte digitale Touch ID,
 * reconnaissance faciale Face ID, cle Passkey WebAuthn / FIDO2).
 */
@Service
public class BiometricAuthenticationService implements BiometricAuthenticationUseCase {

    private static final String BIOMETRIC_CHALLENGE_PURPOSE = "BIOMETRIC";

    private final BiometricCredentialRepository credentialRepository;
    private final BiometricVerifierPort biometricVerifier;
    private final UserRepository userRepository;
    private final JwtTokenPort jwtTokenPort;
    private final SecurityContextPort securityContext;
    private final TokenIssuanceSupport tokenIssuance;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public BiometricAuthenticationService(BiometricCredentialRepository credentialRepository,
                                          BiometricVerifierPort biometricVerifier,
                                          UserRepository userRepository,
                                          JwtTokenPort jwtTokenPort,
                                          SecurityContextPort securityContext,
                                          TokenIssuanceSupport tokenIssuance,
                                          ViewMapper viewMapper,
                                          AuditLogger auditLogger,
                                          DomainEventPublisher eventPublisher,
                                          Clock clock) {
        this.credentialRepository = credentialRepository;
        this.biometricVerifier = biometricVerifier;
        this.userRepository = userRepository;
        this.jwtTokenPort = jwtTokenPort;
        this.securityContext = securityContext;
        this.tokenIssuance = tokenIssuance;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Views.BiometricCredentialView enroll(AuthCommands.EnrollBiometric command) {
        Long userId = securityContext.requireCurrentUserId();
        if (credentialRepository.findByCredentialId(command.credentialId()).isPresent()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Ce credentialId biometrique est deja enregistre");
        }
        BiometricCredential credential = BiometricCredential.enroll(
                userId, command.credentialId(), command.publicKey(),
                command.modality(), command.deviceName(), clock);
        BiometricCredential saved = credentialRepository.save(credential);

        auditLogger.success(userId, AuditLog.Action.BIOMETRIC_REGISTERED,
                "enrolement biometrique " + saved.getModality() + " (" + saved.getDeviceName() + ")");
        eventPublisher.publish(new DomainEvent.BiometricCredentialEnrolled(
                userId, saved.getModality().name(), saved.getDeviceName(), clock.instant()));
        return viewMapper.biometricCredentialView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.BiometricCredentialView> listMyCredentials() {
        Long userId = securityContext.requireCurrentUserId();
        return credentialRepository.findByUserId(userId).stream()
                .map(viewMapper::biometricCredentialView)
                .toList();
    }

    @Override
    @Transactional
    public void revoke(Long credentialId) {
        Long userId = securityContext.requireCurrentUserId();
        BiometricCredential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND,
                        "Facteur biometrique introuvable"));
        if (!credential.getUserId().equals(userId)) {
            throw IamException.of(IamErrorCode.FORBIDDEN,
                    "Ce facteur biometrique appartient a un autre utilisateur");
        }
        credential.revoke(clock);
        credentialRepository.save(credential);
        auditLogger.success(userId, AuditLog.Action.BIOMETRIC_REVOKED,
                "revocation biometrique #" + credentialId);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResults.Challenge issueChallenge(AuthCommands.RequestBiometricChallenge command) {
        BiometricCredential credential = null;
        if (command.credentialId() != null && !command.credentialId().isBlank()) {
            credential = credentialRepository.findByCredentialId(command.credentialId().trim())
                    .filter(c -> !c.isRevoked())
                    .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND,
                            "Aucun facteur biometrique actif pour ce credentialId"));
        }
        User user;
        if (credential != null) {
            user = userRepository.findById(credential.getUserId())
                    .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Compte introuvable"));
        } else if (command.email() != null && !command.email().isBlank()) {
            user = userRepository.findByEmail(command.email().trim().toLowerCase())
                    .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Compte introuvable"));
            boolean hasActiveBiometric = credentialRepository.findByUserId(user.getId()).stream()
                    .anyMatch(c -> !c.isRevoked());
            if (!hasActiveBiometric) {
                throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                        "Aucun facteur biometrique actif n'est enregistre pour ce compte");
            }
        } else {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Indiquez email ou credentialId pour obtenir un defi biometrique");
        }

        user.assertCanAuthenticate(clock);
        String challengeToken = jwtTokenPort.createChallengeToken(user.getId(), BIOMETRIC_CHALLENGE_PURPOSE);
        String target = credential != null ? credential.getCredentialId() : user.getEmail();
        return new AuthResults.Challenge(challengeToken, "BIOMETRIC", target);
    }

    @Override
    @Transactional
    public AuthResults.AuthResult authenticate(AuthCommands.VerifyBiometric command) {
        JwtTokenPort.ChallengeClaims claims =
                jwtTokenPort.parseChallengeToken(command.challengeToken(), BIOMETRIC_CHALLENGE_PURPOSE);
        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> IamException.of(IamErrorCode.TOKEN_INVALID, "Compte introuvable"));
        user.assertCanAuthenticate(clock);

        BiometricCredential credential = credentialRepository.findByCredentialId(command.credentialId())
                .orElseThrow(() -> IamException.of(IamErrorCode.INVALID_CREDENTIALS,
                        "Facteur biometrique inconnu"));
        if (!credential.getUserId().equals(user.getId()) || credential.isRevoked()) {
            throw IamException.of(IamErrorCode.INVALID_CREDENTIALS,
                    "Facteur biometrique invalide ou revoque");
        }

        boolean verified = biometricVerifier.verifyAssertion(
                credential.getPublicKey(), command.challengeToken(),
                credential.getCredentialId(), command.signature());
        if (!verified) {
            auditLogger.failure(user.getId(), AuditLog.Action.LOGIN_FAILED,
                    "signature biometrique invalide (" + credential.getModality() + ")",
                    command.metadata() == null ? null : command.metadata().ipAddress());
            throw IamException.of(IamErrorCode.INVALID_CREDENTIALS,
                    "Assertion biometrique invalide");
        }

        credential.recordAssertion(command.signCount(), clock);
        credentialRepository.save(credential);

        RequestMetadata metadata = command.metadata() == null
                ? RequestMetadata.unknown() : command.metadata();
        AuthResults.TokenSet tokens = tokenIssuance.completeLogin(user, metadata, false);
        auditLogger.success(user.getId(), AuditLog.Action.BIOMETRIC_LOGIN,
                "connexion biometrique " + credential.getModality() + " (" + credential.getDeviceName() + ")");
        return AuthResults.AuthResult.authenticated(tokens);
    }
}
