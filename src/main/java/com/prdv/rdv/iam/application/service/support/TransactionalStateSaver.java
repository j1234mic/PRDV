package com.prdv.rdv.iam.application.service.support;

import com.prdv.rdv.iam.application.port.output.AuditLogRepository;
import com.prdv.rdv.iam.application.port.output.OtpChallengeRepository;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sauvegardes d'etat qui DOIVENT etre commitees meme lorsque la transaction
 * appelante se termine par une exception fonctionnelle (et donc un rollback).
 *
 * <p>Typiquement : compteur d'echecs de connexion (sinon le verrouillage au
 * 5e echec ne fonctionne jamais), compteur de tentatives OTP incorrectes
 * (sinon la limite de 3 essais ne s'applique jamais), revoquation des
 * refresh tokens lors d'une detection de reutilisation, ou trace d'audit
 * de securite qui doit survivre a l'erreur renvoyee au client.</p>
 *
 * <p>Ces methodes s'executent dans une transaction independante
 * ({@code REQUIRES_NEW}) : leur commit n'est pas affecte par le rollback
 * de la transaction appelante.</p>
 */
@Component
public class TransactionalStateSaver {

    private final UserRepository userRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuditLogRepository auditLogRepository;

    public TransactionalStateSaver(UserRepository userRepository,
                                   OtpChallengeRepository otpChallengeRepository,
                                   PatientProfileRepository patientProfileRepository,
                                   RefreshTokenRepository refreshTokenRepository,
                                   UserSessionRepository userSessionRepository,
                                   AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.otpChallengeRepository = otpChallengeRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userSessionRepository = userSessionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OtpChallenge saveOtpChallenge(OtpChallenge challenge) {
        return otpChallengeRepository.save(challenge);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientProfile savePatientProfile(PatientProfile profile) {
        return patientProfileRepository.save(profile);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefreshToken saveRefreshToken(RefreshToken token) {
        return refreshTokenRepository.save(token);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserSession saveUserSession(UserSession session) {
        return userSessionRepository.save(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog saveAuditLog(AuditLog log) {
        return auditLogRepository.save(log);
    }
}
