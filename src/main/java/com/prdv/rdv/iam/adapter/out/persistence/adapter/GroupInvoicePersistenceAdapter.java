package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.mapper.VerificationPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.GroupInvoiceJpaRepository;
import com.prdv.rdv.iam.application.port.output.GroupInvoiceRepository;
import com.prdv.rdv.iam.domain.model.verification.GroupInvoice;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class GroupInvoicePersistenceAdapter implements GroupInvoiceRepository {

    private final GroupInvoiceJpaRepository jpa;
    private final VerificationPersistenceMapper mapper;

    public GroupInvoicePersistenceAdapter(GroupInvoiceJpaRepository jpa,
                                          VerificationPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public GroupInvoice save(GroupInvoice invoice) {
        return mapper.toDomain(jpa.save(mapper.toEntity(invoice)));
    }

    @Override
    public Optional<GroupInvoice> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<GroupInvoice> findByEstablishmentUserId(Long establishmentUserId) {
        return jpa.findByEstablishmentUserIdOrderByCreatedAtDesc(establishmentUserId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
