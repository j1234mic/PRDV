package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.EstablishmentMembershipEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.VerificationPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.MembershipJpaRepository;
import com.prdv.rdv.iam.application.port.output.MembershipRepository;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapteur JPA des rattachements praticien / etablissement
 * (roles OWNER, EMPLOYEE, REPLACER, multi-cabinets).
 */
@Repository
public class MembershipPersistenceAdapter implements MembershipRepository {

    private final MembershipJpaRepository membershipJpa;
    private final VerificationPersistenceMapper mapper;

    public MembershipPersistenceAdapter(MembershipJpaRepository membershipJpa,
                                        VerificationPersistenceMapper mapper) {
        this.membershipJpa = membershipJpa;
        this.mapper = mapper;
    }

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
