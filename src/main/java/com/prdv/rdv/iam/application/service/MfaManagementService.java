package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.port.input.MfaManagementUseCase;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.TotpPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Gestion du second facteur TOTP (RFC 6238) : provisionnement du secret,
 * activation apres verification du premier code, desactivation.
 */
@Service
public class MfaManagementService implements MfaManagementUseCase {

    private final UserRepository userRepository;
    private final TotpPort totpPort;
    private final SecurityContextPort securityContext;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public MfaManagementService(UserRepository userRepository, TotpPort totpPort,
                                SecurityContextPort securityContext, AuditLogger auditLogger, Clock clock) {
        this.userRepository = userRepository;
        this.totpPort = totpPort;
        this.securityContext = securityContext;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthResults.MfaSetup beginSetup() {
        User user = currentUser();
        String secret = totpPort.generateSecret();
        user.storePendingMfaSecret(secret, clock);
        userRepository.save(user);
        return new AuthResults.MfaSetup(secret, totpPort.provisioningUri(secret, user.getEmail()));
    }

    @Override
    @Transactional
    public void enable(String code) {
        User user = currentUser();
        String pending = user.getPendingTotpSecret();
        if (pending == null) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Aucune configuration MFA en cours, appelez d'abord l'etape de preparation");
        }
        if (!totpPort.verifyCode(pending, code)) {
            throw IamException.of(IamErrorCode.MFA_INVALID_CODE, "Code TOTP invalide");
        }
        user.enableMfa(clock);
        userRepository.save(user);
        auditLogger.success(user.getId(), AuditLog.Action.MFA_ENABLED, "activation TOTP");
    }

    @Override
    @Transactional
    public void disable() {
        User user = currentUser();
        user.disableMfa(clock);
        userRepository.save(user);
        auditLogger.success(user.getId(), AuditLog.Action.MFA_DISABLED, "desactivation TOTP");
    }

    private User currentUser() {
        Long id = securityContext.requireCurrentUserId();
        return userRepository.findById(id)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
    }
}
