package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.user.PractitionerProfile;

import java.util.List;
import java.util.Optional;

public interface PractitionerProfileRepository {

    PractitionerProfile save(PractitionerProfile profile);

    Optional<PractitionerProfile> findByUserId(Long userId);

    /** Praticiens dont le compte attend la validation manuelle d'un moderateur. */
    List<PractitionerProfile> findPendingValidation();
}
