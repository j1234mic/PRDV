package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.security.E2eeUserKey;

import java.util.List;
import java.util.Optional;

public interface E2eeKeyRepository {

    E2eeUserKey save(E2eeUserKey key);

    Optional<E2eeUserKey> findActiveByUserId(Long userId);

    List<E2eeUserKey> findByUserId(Long userId);
}
