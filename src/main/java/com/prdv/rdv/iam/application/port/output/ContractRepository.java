package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.verification.PractitionerContract;

import java.util.List;
import java.util.Optional;

public interface ContractRepository {

    PractitionerContract save(PractitionerContract contract);

    Optional<PractitionerContract> findLatestByPractitionerUserId(Long practitionerUserId);

    boolean existsAcceptedByPractitionerUserId(Long practitionerUserId);

    default List<PractitionerContract> findAllByPractitionerUserId(Long practitionerUserId) {
        return findLatestByPractitionerUserId(practitionerUserId).map(List::of).orElseGet(List::of);
    }
}
