package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.mapper.VerificationPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.ContractJpaRepository;
import com.prdv.rdv.iam.application.port.output.ContractRepository;
import com.prdv.rdv.iam.domain.model.verification.PractitionerContract;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapteur JPA des contrats praticien (contrat signe electroniquement,
 * preuve d'acceptation horodatee et hachee).
 */
@Repository
public class ContractPersistenceAdapter implements ContractRepository {

    private final ContractJpaRepository contractJpa;
    private final VerificationPersistenceMapper mapper;

    public ContractPersistenceAdapter(ContractJpaRepository contractJpa,
                                      VerificationPersistenceMapper mapper) {
        this.contractJpa = contractJpa;
        this.mapper = mapper;
    }

    @Override
    public PractitionerContract save(PractitionerContract contract) {
        return mapper.toDomain(contractJpa.save(mapper.toEntity(contract)));
    }

    @Override
    public Optional<PractitionerContract> findLatestByPractitionerUserId(Long practitionerUserId) {
        return contractJpa.findTopByPractitionerUserIdOrderByAcceptedAtDesc(practitionerUserId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsAcceptedByPractitionerUserId(Long practitionerUserId) {
        return contractJpa.existsByPractitionerUserId(practitionerUserId);
    }
}
