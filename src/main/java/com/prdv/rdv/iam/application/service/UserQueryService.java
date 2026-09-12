package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.port.input.UserQueryUseCase;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserQueryService implements UserQueryUseCase {

    private final UserRepository userRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;

    public UserQueryService(UserRepository userRepository, SecurityContextPort securityContext,
                            ViewMapper viewMapper) {
        this.userRepository = userRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Views.UserView currentUser() {
        Long id = securityContext.requireCurrentUserId();
        User user = userRepository.findById(id)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        return viewMapper.userView(user);
    }
}
