package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.port.input.UserAdministrationUseCase;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Gestion administrative transverse des comptes. */
@Service
public class UserAdministrationService implements UserAdministrationUseCase {

    private final UserRepository userRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public UserAdministrationService(UserRepository userRepository, SecurityContextPort securityContext,
                                     ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this.userRepository = userRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PagedResult<Views.UserView> listUsers(int page, int size) {
        if (page < 0 || size <= 0 || size > 200) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Pagination invalide");
        }
        var users = userRepository.findAll(page, size);
        return new Views.PagedResult<>(viewMapper.userViews(users), userRepository.count(), page, size);
    }

    @Override
    @Transactional
    public Views.UserView changeAccountStatus(AdminCommands.ChangeAccountStatus command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        if (command.suspend()) {
            user.suspend(clock);
            auditLogger.success(securityContext.currentUserId().orElse(null),
                    AuditLog.Action.ACCOUNT_SUSPENDED, "compte #" + user.getId() + " suspendu");
        } else {
            user.activate(clock);
            auditLogger.success(securityContext.currentUserId().orElse(null),
                    AuditLog.Action.ACCOUNT_ACTIVATED, "compte #" + user.getId() + " reactive");
        }
        return viewMapper.userView(userRepository.save(user));
    }
}
