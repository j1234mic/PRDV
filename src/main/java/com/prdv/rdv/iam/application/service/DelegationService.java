package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.port.input.DelegationManagementUseCase;
import com.prdv.rdv.iam.application.port.output.DelegationRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.Delegation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Delegation temporaire de permissions (ex : medecin -> secretaire pendant
 * une absence). Les delegations actives sont prises en compte en direct par
 * {@code AuthorizationQueryService} (revocation immediate).
 */
@Service
public class DelegationService implements DelegationManagementUseCase {

    private final DelegationRepository delegationRepository;
    private final UserRepository userRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public DelegationService(DelegationRepository delegationRepository, UserRepository userRepository,
                             SecurityContextPort securityContext, ViewMapper viewMapper,
                             AuditLogger auditLogger, Clock clock) {
        this.delegationRepository = delegationRepository;
        this.userRepository = userRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Views.DelegationView grant(AdminCommands.GrantDelegation command) {
        Long granterId = securityContext.requireCurrentUserId();
        userRepository.findById(command.granteeUserId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Delegataire introuvable"));

        Delegation delegation = Delegation.grant(granterId, command.granteeUserId(),
                command.permissionCodes(), command.reason(), command.validFrom(), command.validUntil(), clock);
        Delegation saved = delegationRepository.save(delegation);
        auditLogger.success(granterId, AuditLog.Action.DELEGATION_GRANTED,
                "vers #" + command.granteeUserId() + " : " + command.permissionCodes());
        return viewMapper.delegationView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.DelegationView> delegationsForMe() {
        Long userId = securityContext.requireCurrentUserId();
        List<Delegation> all = new ArrayList<>(delegationRepository.findByGranter(userId));
        delegationRepository.findActiveByGrantee(userId, clock.instant()).stream()
                .filter(d -> !d.getGranterUserId().equals(userId))
                .forEach(all::add);
        return all.stream().map(viewMapper::delegationView).toList();
    }

    @Override
    @Transactional
    public void revoke(Long delegationId) {
        Long userId = securityContext.requireCurrentUserId();
        Delegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Delegation introuvable"));
        if (!delegation.getGranterUserId().equals(userId)) {
            throw IamException.of(IamErrorCode.FORBIDDEN,
                    "Seul le deleguant peut revoquer une delegation");
        }
        delegation.revoke(clock);
        delegationRepository.save(delegation);
        auditLogger.success(userId, AuditLog.Action.DELEGATION_REVOKED, "delegation #" + delegationId);
    }
}
