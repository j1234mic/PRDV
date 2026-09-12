package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.user.SecretaryProfile;

import java.util.List;
import java.util.Optional;

public interface SecretaryProfileRepository {

    SecretaryProfile save(SecretaryProfile profile);

    Optional<SecretaryProfile> findByUserId(Long userId);

    List<SecretaryProfile> findBySupervisedPractitionerId(Long practitionerUserId);
}
