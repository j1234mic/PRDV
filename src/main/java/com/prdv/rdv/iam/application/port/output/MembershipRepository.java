package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository {

    EstablishmentMembership save(EstablishmentMembership membership);

    Optional<EstablishmentMembership> findById(Long id);

    List<EstablishmentMembership> findByEstablishmentUserId(Long establishmentUserId);

    List<EstablishmentMembership> findByPractitionerUserId(Long practitionerUserId);
}
