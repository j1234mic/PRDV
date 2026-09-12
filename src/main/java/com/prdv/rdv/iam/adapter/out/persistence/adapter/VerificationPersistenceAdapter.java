package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.EstablishmentMembershipEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.KycDocumentEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.PractitionerContractEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.VerificationPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.ContractJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.KycDocumentJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.MembershipJpaRepository;
import com.prdv.rdv.iam.application.port.output.ContractRepository;
import com.prdv.rdv.iam.application.port.output.KycDocumentRepository;
import com.prdv.rdv.iam.application.port.output.MembershipRepository;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import com.prdv.rdv.iam.domain.model.verification.PractitionerContract;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VerificationPersistenceAdapter
        implements KycDocumentRepository, ContractRepository, MembershipRepository {

    private final KycDocumentJpaRepository kycJpa;
    private final ContractJpaRepository contractJpa;
    private final MembershipJpaRepository membershipJpa;
    private final VerificationPersistenceMapper mapper;

    public VerificationPersistenceAdapter(KycDocumentJpaRepository kycJpa,
                                          ContractJpaRepository contractJpa,
                                          MembershipJpaRepository membershipJpa,
                                          VerificationPersistenceMapper mapper) {
        this.kycJpa = kycJpa;
        this.contractJpa = contractJpa;
        this.membershipJpa = membershipJpa;
        this.mapper = mapper;
    }

    // ----------------------------------------------------------------- KYC
    @Override
    public KycDocument save(KycDocument document) {
        KycDocumentEntity existing = document.getId() == null ? null
                : kycJpa.findById(document.getId()).orElse(null);
        KycDocumentEntity mapped = mapper.toEntity(document);
        if (existing != null) {
            mapped.setId(existing.getId());
            mapped.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(kycJpa.save(mapped));
    }

    @Override
    public Optional<KycDocument> findById(Long id) {
        return kycJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<KycDocument> findByOwnerUserId(Long ownerUserId) {
        return kycJpa.findByOwnerUserIdOrderByUploadedAtDesc(ownerUserId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<KycDocument> findPending(int page, int size) {
        return kycJpa.findByStatusOrderByUploadedAtAsc(KycDocument.ReviewStatus.PENDING,
                        PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }

    // ------------------------------------------------------------ Contrat
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

    // --------------------------------------------------------- Membership
    @Override
    public EstablishmentMembership save(EstablishmentMembership membership) {
        EstablishmentMembershipEntity existing = membership.getId() == null ? null
                : membershipJpa.findById(membership.getId()).orElse(null);
        EstablishmentMembershipEntity mapped = mapper.toEntity(membership);
        if (existing != null) {
            mapped.setId(existing.getId());
            mapped.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(membershipJpa.save(mapped));
    }

    @Override
    public Optional<EstablishmentMembership> findById(Long id) {
        return membershipJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<EstablishmentMembership> findByEstablishmentUserId(Long establishmentUserId) {
        return membershipJpa.findByEstablishmentUserId(establishmentUserId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<EstablishmentMembership> findByPractitionerUserId(Long practitionerUserId) {
        return membershipJpa.findByPractitionerUserId(practitionerUserId).stream()
                .map(mapper::toDomain).toList();
    }
}
