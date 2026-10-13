package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.mapper.SecurityPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.BugBountyReportJpaRepository;
import com.prdv.rdv.iam.application.port.output.BugBountyReportRepository;
import com.prdv.rdv.iam.domain.model.security.BugBountyReport;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BugBountyReportPersistenceAdapter implements BugBountyReportRepository {

    private final BugBountyReportJpaRepository jpa;
    private final SecurityPersistenceMapper mapper;

    public BugBountyReportPersistenceAdapter(BugBountyReportJpaRepository jpa,
                                             SecurityPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public BugBountyReport save(BugBountyReport report) {
        return mapper.toDomain(jpa.save(mapper.toEntity(report)));
    }

    @Override
    public Optional<BugBountyReport> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<BugBountyReport> findAll() {
        return jpa.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
