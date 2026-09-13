package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.AuditLogJpaRepository;
import com.prdv.rdv.iam.application.port.output.AuditLogRepository;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Adapteur JPA du journal d'audit (traces immuables : connexions, MFA, roles,
 * KYC, delegations, anonymisation).
 */
@Repository
public class AuditLogPersistenceAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository auditJpa;
    private final AuthPersistenceMapper mapper;

    public AuditLogPersistenceAdapter(AuditLogJpaRepository auditJpa, AuthPersistenceMapper mapper) {
        this.auditJpa = auditJpa;
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog logEntry) {
        return mapper.toDomain(auditJpa.save(mapper.toEntity(logEntry)));
    }

    @Override
    public List<AuditLog> findAll(int page, int size) {
        return auditJpa.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByUserId(Long userId, int page, int size) {
        return auditJpa.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return auditJpa.count();
    }
}
