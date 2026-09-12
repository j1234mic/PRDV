package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.UserSession;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository {

    UserSession save(UserSession session);

    Optional<UserSession> findById(Long id);

    List<UserSession> findByUserId(Long userId);

    /** Derniere session active : utilisee pour la detection de fraude (nouvel appareil / pays). */
    Optional<UserSession> findLatestActiveByUserId(Long userId);
}
