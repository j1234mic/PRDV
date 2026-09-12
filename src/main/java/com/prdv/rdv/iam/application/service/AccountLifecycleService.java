package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.port.input.AccountLifecycleUseCase;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Droit a l'oubli (RGPD) : anonymisation du compte et suppression des
 * donnees personnelles, conservation d'une coquille pour l'historique facture.
 */
@Service
public class AccountLifecycleService implements AccountLifecycleUseCase {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityContextPort securityContext;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public AccountLifecycleService(UserRepository userRepository,
                                   PatientProfileRepository patientProfileRepository,
                                   UserSessionRepository sessionRepository,
                                   RefreshTokenRepository refreshTokenRepository,
                                   SecurityContextPort securityContext,
                                   AuditLogger auditLogger, Clock clock) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityContext = securityContext;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void deleteMyAccount() {
        Long userId = securityContext.requireCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        for (UserSession session : sessionRepository.findByUserId(userId)) {
            if (session.isActive()) {
                session.revoke(clock);
                sessionRepository.save(session);
            }
        }
        for (RefreshToken token : refreshTokenRepository.findByUserId(userId)) {
            if (token.isValid(clock)) {
                token.revoke(clock);
                refreshTokenRepository.save(token);
            }
        }

        patientProfileRepository.findByUserId(userId).ifPresent((PatientProfile p) -> {
            p.setFirstName("ANONYMISE");
            p.setLastName("");
            p.setBirthDate(null);
            p.setGender(null);
            p.setGuardianUserId(null);
            p.setIdentityDocumentToken(null);
            patientProfileRepository.save(p);
        });

        user.anonymize(clock);
        userRepository.save(user);
        auditLogger.success(userId, AuditLog.Action.ACCOUNT_ANONYMIZED, "droit a l'oubli");
    }
}
