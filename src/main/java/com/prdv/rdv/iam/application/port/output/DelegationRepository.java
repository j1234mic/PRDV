package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.Delegation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DelegationRepository {

    Delegation save(Delegation delegation);

    Optional<Delegation> findById(Long id);

    /** Delegations actives a un instant donne pour le delegataire. */
    List<Delegation> findActiveByGrantee(Long granteeUserId, Instant now);

    List<Delegation> findByGranter(Long granterUserId);
}
