package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.port.input.SessionUseCase;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;

/**
 * Gestion des sessions connectees : liste des appareils, revocation ciblee
 * ou globale (couplee a la version de tokens pour une revocation immediate).
 */
@Service
public class SessionService implements SessionUseCase {

    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public SessionService(UserSessionRepository sessionRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          UserRepository userRepository,
                          SecurityContextPort securityContext,
                          ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.SessionView> listMySessions() {
        Long userId = securityContext.requireCurrentUserId();
        List<UserSession> sessions = sessionRepository.findByUserId(userId);
        Long currentId = sessions.stream()
                .filter(UserSession::isActive)
                .max(Comparator.comparing(UserSession::getLastSeenAt))
                .map(UserSession::getId).orElse(null);
        return sessions.stream()
                .sorted(Comparator.comparing(UserSession::getCreatedAt).reversed())
                .map(s -> viewMapper.sessionView(s, s.getId().equals(currentId)))
                .toList();
    }

    @Override
    @Transactional
    public void revoke(Long sessionId) {
        Long userId = securityContext.requireCurrentUserId();
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Session introuvable"));
        if (!session.getUserId().equals(userId)) {
            throw IamException.of(IamErrorCode.FORBIDDEN, "Session appartenant a un autre utilisateur");
        }
        revokeSessionAndTokens(session);
        auditLogger.record(userId, AuditLog.Action.SESSION_REVOKED, AuditLog.Outcome.SUCCESS,
                "Session", String.valueOf(sessionId), "revelation de session", session.getIpAddress());
    }

    @Override
    @Transactional
    public void revokeAll() {
        Long userId = securityContext.requireCurrentUserId();
        sessionRepository.findByUserId(userId).stream()
                .filter(UserSession::isActive)
                .forEach(this::revokeSessionAndTokens);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        // Incremente la version : tous les access tokens JWT emis deviennent invalides
        user.revokeAllTokens(clock);
        userRepository.save(user);
        auditLogger.success(userId, AuditLog.Action.SESSION_REVOKED, "revelation de toutes les sessions");
    }

    private void revokeSessionAndTokens(UserSession session) {
        if (session.isActive()) {
            session.revoke(clock);
            sessionRepository.save(session);
        }
        for (RefreshToken token : refreshTokenRepository.findByUserId(session.getUserId())) {
            if (session.getId().equals(token.getSessionId()) && token.isValid(clock)) {
                token.revoke(clock);
                refreshTokenRepository.save(token);
            }
        }
    }
}
