package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;

import java.util.List;
import java.util.Optional;

public interface EstablishmentProfileRepository {

    EstablishmentProfile save(EstablishmentProfile profile);

    Optional<EstablishmentProfile> findById(Long id);

    Optional<EstablishmentProfile> findByUserId(Long userId);

    List<EstablishmentProfile> findAll();

    List<EstablishmentProfile> findPendingValidation();
}
